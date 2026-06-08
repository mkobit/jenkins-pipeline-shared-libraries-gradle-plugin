@file:Suppress("ktlint:standard:no-wildcard-imports", "UnstableApiUsage")

package com.mkobit.jenkins.pipelines

import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.plugins.quality.CodeNarc
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.language.base.plugins.LifecycleBasePlugin
import kotlin.io.path.outputStream

plugins {
  groovy
  `jvm-test-suite`
  codenarc
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(17)
  }
}

dependencies {
  components {
    all<JenkinsPluginRule>()
    withModule<JenkinsTestHarnessServletApiRule>(
      "org.jenkins-ci.main:jenkins-test-harness",
    )
  }

  attributesSchema.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE) {
    compatibilityRules.add(JpiCompatibilityRule::class)
  }
  attributesSchema.attribute(JenkinsPluginRule.JENKINS_ARTIFACT_ATTRIBUTE) {
    disambiguationRules.add(JenkinsArtifactDisambiguationRule::class)
  }
}

val srcDir = layout.projectDirectory.dir("src")
val varsDir = layout.projectDirectory.dir("vars")
val resourcesDir = layout.projectDirectory.dir("resources")

// Register HPI, WAR, and groovyAll configurations early (no properties set yet) so that
// the providers below can reference them before the extension is created.
// Properties are configured later via .configure { } after sharedLibrary exists.
val jenkinsPluginHpis = configurations.register(JENKINS_PLUGIN_HPIS_CONFIGURATION)
val jenkinsWar = configurations.register(JENKINS_WAR_CONFIGURATION)
val groovyAllRuntime = configurations.register(GROOVY_ALL_RUNTIME_CONFIGURATION)

// Lenient view so plain-JAR transitives that don't publish HPI are silently skipped
// rather than failing resolution when artifactType=hpi is requested globally.
// JpiCompatibilityRule makes plain JARs compatible with the HPI request; filter to actual
// .hpi/.jpi files so transitive JARs (e.g. groovy-all) don't leak onto the test classpath.
val hpiFiles: Provider<FileCollection> =
  jenkinsPluginHpis.map { cfg ->
    cfg.incoming
      .artifactView { isLenient = true }
      .artifacts
      .artifactFiles
      .filter { it.name.endsWith(".hpi") || it.name.endsWith(".jpi") }
  }
val jenkinsWarFile: Provider<File> =
  jenkinsWar.map { cfg -> cfg.files.single { it.extension == "war" } }

// Create the source set early so its output is available when the Jenkins wiring configureEach
// block fires. Src dirs are wired later once generateLocalLibraryFiles is registered.
val localLibraryRetrieverSourceSet =
  sourceSets.create(LOCAL_LIBRARY_RETRIEVER_SOURCE_SET)
localLibraryRetrieverSourceSet.groovy.setSrcDirs(emptyList<Any>())

abstract class JenkinsTestSuiteService : BuildService<BuildServiceParameters.None>

val sharedLibrary =
  extensions
    .create<SharedLibraryExtension>("sharedLibrary")
    .apply {
      jenkins.version.convention(SharedLibraryDefaults.CORE_VERSION)
      jenkins.bomVersion.convention(SharedLibraryDefaults.BOM_VERSION)
      pipelineUnitVersion.convention(SharedLibraryDefaults.PIPELINE_UNIT_VERSION)
      autoRegisterLibrary.convention(true)
      implicit.convention(true)
      libraryName.convention(project.name)
      maxParallelJenkinsTests.convention(1)
    }

val jenkinsTestSuiteService =
  gradle.sharedServices.registerIfAbsent("jenkinsTestSuite", JenkinsTestSuiteService::class.java) {
    maxParallelUsages = sharedLibrary.maxParallelJenkinsTests
  }

// Each library's source is synced under its own named subdirectory so N libraries can
// coexist without collisions: build/sharedLibrarySource/{libraryName}/{src,vars,resources}.
// This naming convention is the foundation for eventual multi-library support — external
// libraries resolved from Gradle dependencies will land in sibling directories.
val sharedLibrarySourceDir: Provider<Directory> =
  sharedLibrary.libraryName.flatMap { name ->
    layout.buildDirectory.dir("sharedLibrarySource/$name")
  }

val syncSharedLibrarySource =
  tasks.register<SyncSharedLibrarySource>("syncSharedLibrarySource") {
    description = "Copies shared library source (src/, vars/, resources/) into build/sharedLibrarySource/{libraryName}/"
    group = LifecycleBasePlugin.BUILD_GROUP
    srcFiles.from(srcDir)
    varsFiles.from(varsDir)
    resourcesFiles.from(resourcesDir)
    destinationDir = sharedLibrarySourceDir
  }

// Outgoing variant: exposes the Sync task output as a resolvable artifact so other Gradle
// projects can declare this project as a shared library source dependency.
// Variant attribute Category="shared-library-source" identifies it as library source, not code.
configurations.register(SHARED_LIBRARY_SOURCE_ELEMENTS_CONFIGURATION) {
  isCanBeResolved = false
  isCanBeConsumed = true
  description = "Shared library source files for consumption by dependent projects via variant-aware resolution"
  attributes {
    attribute(Category.CATEGORY_ATTRIBUTE, objects.named<Category>(SHARED_LIBRARY_SOURCE_CATEGORY))
    attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>(SHARED_LIBRARY_SOURCE_USAGE))
  }
  outgoing.artifact(syncSharedLibrarySource.flatMap { it.destinationDir }) {
    type = "directory"
    builtBy(syncSharedLibrarySource)
  }
}

val jenkinsBom =
  configurations.register(JENKINS_BOM_CONFIGURATION) {
    isCanBeResolved = false
    isCanBeConsumed = false
    description = "Jenkins BOM platform — version alignment for all Jenkins artifacts"
  }
dependencies {
  // BOM is optional: absent when bomVersion is unset (consumer opted out).
  jenkinsBom(
    sharedLibrary.jenkins.bomVersion.flatMap { bomVer ->
      sharedLibrary.jenkins.version.map { v ->
        val (major, minor) = v.split(".")
        project.dependencies.platform("io.jenkins.tools.bom:bom-$major.$minor.x:$bomVer")
      }
    },
  )
}

val jenkinsPlugin =
  configurations.register(JENKINS_PLUGIN_CONFIGURATION) {
    isCanBeResolved = false
    isCanBeConsumed = false
    description = "Jenkins HPI/JPI plugin dependencies for shared library compilation and testing"
    extendsFrom(jenkinsBom)
    fromDependencyCollector(sharedLibrary.plugins.pluginCollector)
  }
dependencies {
  jenkinsPlugin(sharedLibrary.jenkins.version.map { v -> "org.jenkins-ci.main:jenkins-core:$v" })
  jenkinsPlugin(PIPELINE_GROOVY_LIB_MODULE)
  jenkinsPlugin(WORKFLOW_JOB_MODULE)
  jenkinsPlugin(WORKFLOW_BASIC_STEPS_MODULE)
  jenkinsPlugin(WORKFLOW_DURABLE_TASK_STEP_MODULE)
}

jenkinsPluginHpis.configure {
  isCanBeResolved = true
  isCanBeConsumed = false
  description = "Jenkins plugin HPI archives for embedded Jenkins runtime (integration tests)"
  extendsFrom(jenkinsPlugin)
  attributes {
    attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "hpi")
    attribute(JenkinsPluginRule.JENKINS_ARTIFACT_ATTRIBUTE, "hpi")
  }
}
jenkinsWar.configure {
  isCanBeResolved = true
  isCanBeConsumed = false
  description = "Jenkins WAR file for the embedded Jenkins runtime (integration tests)"
}
dependencies {
  jenkinsWar(sharedLibrary.jenkins.version.map { v -> "org.jenkins-ci.main:jenkins-war:$v@war" })
}

sourceSets.main.configure {
  java.setSrcDirs(emptyList<String>())
  groovy.setSrcDirs(listOf("src", "vars"))
  resources.setSrcDirs(listOf("resources"))
}
// Jenkins APIs are compile-only for the shared library; the library runs inside Jenkins at runtime.
configurations.compileOnly {
  extendsFrom(jenkinsPlugin)
}

// Integration tests need groovy-all at *runtime only* so SandboxInterceptor
// (script-security plugin) can load SqlGroovyMethods and other Groovy 2.4 DGM classes
// that no longer exist in the groovy-3.x module jars. An isolated configuration bypasses
// the groovy-all exclusion on implementation, keeping it off the compile classpath.
//
// FUTURE: this injection is currently unconditional. Once a Jenkins version removes
// groovy-all from the WAR (Jenkins has not done so through 2.541.1 as of 2026-05-04 —
// see `findGroovyAllThreshold` in the root project), this should become conditional via a
// ComponentMetadataRule on jenkins-core: resolve the WAR artifact for the selected Jenkins
// version, inspect WEB-INF/lib/ for groovy-all-*.jar, and skip adding groovyAllRuntime when
// it is absent. Design sketch:
//   dependencies.components.withModule("org.jenkins-ci.main:jenkins-core", GroovyAllCapabilityRule::class.java)
// where GroovyAllCapabilityRule adds a "provides groovy-all" capability to variants where the
// WAR bundles it, and the integrationTestGroovyAllRuntime configuration requires that
// capability — making Gradle skip the add when jenkins-core no longer satisfies it.
// Track via GitHub issue (file after this branch merges). See docs/06-backlog.md M7.
groovyAllRuntime.configure {
  isCanBeResolved = true
  isCanBeConsumed = false
}
dependencies {
  groovyAllRuntime(SharedLibraryDefaults.GROOVY_ALL_COORDINATES)
}

val generateLocalLibraryFiles =
  tasks.register<GenerateLocalLibraryFiles>("generateLocalLibraryFiles") {
    description = "Generates LocalLibraryRetriever and SharedLibraryAutoRegistrar source files"
    javaOutputDir = layout.buildDirectory.dir("generated-src/localLibraryRetriever/java")
    resourcesOutputDir = layout.buildDirectory.dir("generated-src/localLibraryRetriever/resources")
    generateAutoRegistrar = sharedLibrary.autoRegisterLibrary
  }

// Dedicated source set for generated helper classes (LocalLibraryRetriever,
// SharedLibraryAutoRegistrar). Kept separate from integrationTest so the generated
// artifacts are compiled once and shared across all Jenkins test suites,
// rather than re-compiled per suite.
localLibraryRetrieverSourceSet.java.setSrcDirs(listOf(generateLocalLibraryFiles.flatMap { it.javaOutputDir }))
localLibraryRetrieverSourceSet.resources.setSrcDirs(listOf(generateLocalLibraryFiles.flatMap { it.resourcesOutputDir }))

// Jenkins APIs are needed to compile LocalLibraryRetriever / SharedLibraryAutoRegistrar.
configurations.named(localLibraryRetrieverSourceSet.compileOnlyConfigurationName) {
  extendsFrom(jenkinsPlugin)
}

testing {
  suites {
    named<JvmTestSuite>("test") {
      useJUnitJupiter()
      sources {
        java.setSrcDirs(listOf("test/unit/java"))
        groovy.setSrcDirs(listOf("test/unit/groovy"))
        resources.setSrcDirs(listOf("test/unit/resources"))
      }
      dependencies {
        implementation(sharedLibrary.pipelineUnitVersion.map { v -> dependencyFactory.create("com.lesfurets:jenkins-pipeline-unit:$v") })
        runtimeOnly(SharedLibraryDefaults.IVY_COORDINATES)
      }
    }
  }
}

val integrationTestSuite =
  testing.suites.register<JvmTestSuite>(INTEGRATION_TEST_SUITE) {
    useJUnitJupiter("6.0.1")
    sources {
      java.setSrcDirs(listOf("test/integration/java"))
      groovy.setSrcDirs(listOf("test/integration/groovy"))
      resources.setSrcDirs(listOf("test/integration/resources"))
    }
    jenkins.enabled.set(true)
  }
// The integrationTest suite's configurations are created at registration time (not realization
// time), so deps can be added directly here and will appear as declared in
// integrationTestImplementation.dependencies without requiring suite realization.
dependencies {
  add("${INTEGRATION_TEST_SUITE}Implementation", "org.jenkins-ci.main:jenkins-test-harness:${SharedLibraryDefaults.TEST_HARNESS_VERSION}")
  add("${INTEGRATION_TEST_SUITE}Implementation", localLibraryRetrieverSourceSet.output)
  add("${INTEGRATION_TEST_SUITE}RuntimeOnly", SharedLibraryDefaults.IVY_COORDINATES)
}

// JvmTestSuite and Project both extend ExtensionAware. Inside this lambda Kotlin resolves
// bare `extensions` to Project.extensions (outer scope). All suite extension-container accesses
// use an explicit (this as ExtensionAware) cast to reach the suite's own container.
//
// The enabled check is deferred to withDependencies (resolution time) and targets.configureEach
// (task realization time) so that user-registered suite creation actions — which call
// withJenkins() or set jenkins.enabled = true — have already run by the time these hooks fire.
testing.suites.withType<JvmTestSuite>().configureEach {
  val suiteExtensions = (this as ExtensionAware).extensions
  val implConfigName = sources.implementationConfigurationName

  project.configurations.named(implConfigName) {
    extendsFrom(jenkinsPlugin)
    exclude(mapOf("group" to "org.codehaus.groovy", "module" to "groovy-all"))
  }
  dependencies {
    implementation(SharedLibraryDefaults.GROOVY_COORDINATES)
  }

  project.configurations.named(implConfigName) {
    withDependencies {
      val ext = suiteExtensions.findByType(JenkinsTestSuiteExtension::class.java) ?: return@withDependencies
      if (!ext.enabled.getOrElse(false)) return@withDependencies
      add(project.dependencies.create("org.jenkins-ci.main:jenkins-test-harness:${SharedLibraryDefaults.TEST_HARNESS_VERSION}"))
      add(project.dependencies.create(localLibraryRetrieverSourceSet.output))
      add(project.dependencies.create(SharedLibraryDefaults.IVY_COORDINATES))
    }
  }

  targets.configureEach {
    testTask.configure {
      val ext = suiteExtensions.findByType(JenkinsTestSuiteExtension::class.java) ?: return@configure
      if (!ext.enabled.getOrElse(false)) return@configure

      val suiteJenkinsDir = layout.buildDirectory.dir("jenkins-for-test/$name")
      mustRunAfter(tasks.test)
      usesService(jenkinsTestSuiteService)
      jvmArgumentProviders.add(
        objects.newInstance<BuildDirJvmArgumentProvider>().apply {
          dir = suiteJenkinsDir
        },
      )
      outputs.dir(suiteJenkinsDir)
      // HPI archives require attribute-based resolution (artifactType=hpi) set up at the
      // project level — must be added via classpath += rather than suite.dependencies.
      classpath += files(hpiFiles)
      // groovy-all:2.4 is required by CpsFlowDefinition at runtime. classpath += bypasses
      // version-conflict resolution intentionally: without it, Gradle would prefer groovy:3.x.
      classpath += files(groovyAllRuntime)
      maxParallelForks = 1
      maxHeapSize = SharedLibraryDefaults.INTEGRATION_TEST_MAX_HEAP_SIZE
      val syncTask = tasks.named<SyncSharedLibrarySource>("syncSharedLibrarySource")
      inputs.files(syncTask).withPropertyName("sharedLibrarySource")
      jvmArgumentProviders.add(
        objects.newInstance<LibraryLocationArgumentProvider>().apply {
          libraryLocation = syncTask.flatMap { it.destinationDir }
        },
      )
      jvmArgumentProviders.add(
        objects.newInstance<JenkinsWarJvmArgumentProvider>().apply {
          warFile.fileProvider(jenkinsWarFile)
        },
      )
      jvmArgumentProviders.add(
        objects.newInstance<LibraryNameArgumentProvider>().apply {
          libraryName.set(sharedLibrary.libraryName)
        },
      )
      jvmArgumentProviders.add(
        objects.newInstance<LibraryImplicitArgumentProvider>().apply {
          implicit.set(sharedLibrary.implicit)
        },
      )
      jvmArgs(
        "-XX:+ExitOnOutOfMemoryError",
        "-XX:+UseG1GC",
      )
      // Jenkins uses XStream, Guice, and other reflection-heavy libraries that
      // require access to JDK internals sealed by Java 9+ strong encapsulation.
      jvmArgs(
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
        "--add-opens=java.base/java.text=ALL-UNNAMED",
        "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
        "--add-opens=java.desktop/java.awt=ALL-UNNAMED",
      )
    }
  }
}

tasks.check {
  dependsOn(integrationTestSuite)
}

tasks {
  register<Jar>("sourcesJar") {
    description = "Assembles a JAR of the source"
    archiveClassifier = "sources"
    from(sourceSets.main.map { it.allSource })
  }
  register<Jar>("groovydocJar") {
    description = "Assembles the Groovydoc JAR"
    archiveClassifier = "javadoc"
    from(tasks.groovydoc.map { it.destinationDir })
  }
}
val ivy =
  configurations.register(IVY_CONFIGURATION) {
    isCanBeResolved = true
    isCanBeConsumed = false
    description = "Ivy for @Grab support in shared library Groovy sources"
  }
dependencies {
  ivy(SharedLibraryDefaults.IVY_COORDINATES)
}
tasks.withType<GroovyCompile>().configureEach {
  groovyClasspath += files(ivy)
}

// Enhanced Classpath Rules (rulesets/jenkins.xml) need Jenkins JARs to resolve
// type hierarchies (Serializable checks, CPS annotations, etc.), and the compiled
// class output so the rules can inspect the actual class hierarchy.
// compileGroovyTask captured outside so the TaskProvider.flatMap overload is used
// (inside configureEach, Kotlin resolves flatMap to Iterable.flatMap instead).
// files() wraps the providers into FileCollection; Gradle traces through them to
// infer the compileGroovy task dependency via @InputFiles — no dependsOn needed.
val compileGroovyTask = tasks.compileGroovy
tasks.withType<CodeNarc>().configureEach {
  compilationClasspath +=
    files(
      sourceSets.main.map { it.compileClasspath },
      compileGroovyTask.flatMap { it.destinationDirectory },
    )
}

// Extract bundled XML configs to build-dir files with .xml extensions so CodeNarc
// parses them as XML rather than as Groovy DSL scripts. getResourceAsStream works
// in both JAR and IDE classpath-directory layouts; fromArchiveEntry would require
// a concrete archive file and fails when resources are unpacked during development.
val jenkinsConfigFile = layout.buildDirectory.file("generated/codenarc/codenarc-jenkins.xml")
val extractJenkinsCodeNarcConfig =
  tasks.register<ExtractJenkinsCodeNarcConfig>("extractJenkinsCodeNarcConfig") {
    group = "build setup"
    description = "Extracts the bundled Jenkins CodeNarc XML ruleset into the build directory."
    outputFile = jenkinsConfigFile
  }
val codenarcJenkinsMain =
  tasks.register<CodeNarc>("codenarcJenkinsMain") {
    description = "Runs Jenkins CPS/Serializable CodeNarc rules against the main source set."
    setSource(sourceSets.main.map { it.groovy })
    dependsOn(extractJenkinsCodeNarcConfig)
    config = resources.text.fromFile(jenkinsConfigFile)
    codenarcClasspath = files(configurations.codenarc)
  }

val defaultCodeNarcConfigFile = layout.buildDirectory.file("generated/codenarc/shared-library-default.xml")
val extractDefaultCodeNarcConfig =
  tasks.register<ExtractDefaultCodeNarcConfig>("extractDefaultCodeNarcConfig") {
    group = "build setup"
    description = "Extracts the bundled default CodeNarc XML ruleset into the build directory."
    outputFile = defaultCodeNarcConfigFile
  }

// Wire the bundled-default config onto every auto-created CodeNarc task except codenarcJenkinsMain.
// The consumer's codenarc.configFile is checked at task configuration time (inside configureEach,
// which runs lazily after all build scripts have been evaluated). If it exists on disk it is used
// directly; otherwise the bundled default is substituted.
tasks.withType<CodeNarc>().configureEach {
  if (name != "codenarcJenkinsMain") {
    dependsOn(extractDefaultCodeNarcConfig)
    val consumerFile = codenarc.configFile
    config =
      if (consumerFile.exists()) {
        resources.text.fromFile(consumerFile)
      } else {
        resources.text.fromFile(defaultCodeNarcConfigFile)
      }
  }
}

tasks.check {
  dependsOn(codenarcJenkinsMain)
}
