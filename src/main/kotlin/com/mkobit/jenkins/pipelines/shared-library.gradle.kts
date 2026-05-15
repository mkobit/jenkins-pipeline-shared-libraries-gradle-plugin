@file:Suppress("ktlint:standard:no-wildcard-imports", "DEPRECATION")

package com.mkobit.jenkins.pipelines

import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.plugins.quality.CodeNarc
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.javadoc.Groovydoc
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.language.base.plugins.LifecycleBasePlugin
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream

plugins {
  groovy
  `jvm-test-suite`
}

val sharedLibrary =
  extensions.create<SharedLibraryExtension>("sharedLibrary").apply {
    jenkins.version.convention(SharedLibraryDefaults.CORE_VERSION)
    jenkins.bomVersion.convention(SharedLibraryDefaults.BOM_VERSION)
    pipelineUnitVersion.convention(SharedLibraryDefaults.PIPELINE_UNIT_VERSION)
    autoRegisterLibrary.convention(true)
    libraryName.convention(project.name)
  }

dependencies {
  components {
    all<JenkinsPluginRule>()
    withModule<JenkinsTestHarnessServletApiRule>(
      "org.jenkins-ci.main:jenkins-test-harness",
    )
  }

  attributesSchema.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE) {
    compatibilityRules.add(JpiCompatibilityRule::class.java)
  }
  attributesSchema.attribute(JenkinsPluginRule.JENKINS_ARTIFACT_ATTRIBUTE) {
    disambiguationRules.add(JenkinsArtifactDisambiguationRule::class.java)
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
    @Suppress("UnstableApiUsage")
    fromDependencyCollector(sharedLibrary.plugins.pluginCollector)
  }
dependencies {
  jenkinsPlugin(sharedLibrary.jenkins.version.map { v -> "org.jenkins-ci.main:jenkins-core:$v" })
  jenkinsPlugin(PIPELINE_GROOVY_LIB_MODULE)
  jenkinsPlugin(WORKFLOW_JOB_MODULE)
  jenkinsPlugin(WORKFLOW_BASIC_STEPS_MODULE)
  jenkinsPlugin(WORKFLOW_DURABLE_TASK_STEP_MODULE)
}

val jenkinsPluginHpis =
  configurations.register(JENKINS_PLUGIN_HPIS_CONFIGURATION) {
    isCanBeResolved = true
    isCanBeConsumed = false
    description = "Jenkins plugin HPI archives for embedded Jenkins runtime (integration tests)"
    extendsFrom(jenkinsPlugin)
    attributes {
      attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "hpi")
      attribute(JenkinsPluginRule.JENKINS_ARTIFACT_ATTRIBUTE, "hpi")
    }
  }

val jenkinsWar =
  configurations.register(JENKINS_WAR_CONFIGURATION) {
    isCanBeResolved = true
    isCanBeConsumed = false
    description = "Jenkins WAR file for the embedded Jenkins runtime (integration tests)"
  }
dependencies {
  jenkinsWar(sharedLibrary.jenkins.version.map { v -> "org.jenkins-ci.main:jenkins-war:$v@war" })
}

// ── Main source set ───────────────────────────────────────────────────────────
sourceSets.main.configure {
  java.setSrcDirs(emptyList<String>())
  groovy.setSrcDirs(listOf("src", "vars"))
  resources.setSrcDirs(listOf("resources"))
}
// Jenkins APIs are compile-only for the shared library; the library runs inside Jenkins at runtime.
configurations.named("compileOnly") {
  extendsFrom(jenkinsPlugin)
}

// ── Test suites ───────────────────────────────────────────────────────────────

// Lenient view so plain-JAR transitives that don't publish HPI are silently skipped
// rather than failing resolution when artifactType=hpi is requested globally.
// JpiCompatibilityRule makes plain JARs compatible with the HPI request; filter to actual
// .hpi/.jpi files so transitive JARs (e.g. groovy-all) don't leak onto the test classpath.
val hpiFiles =
  jenkinsPluginHpis.map { cfg ->
    cfg.incoming
      .artifactView { isLenient = true }
      .artifacts
      .artifactFiles
      .filter { it.name.endsWith(".hpi") || it.name.endsWith(".jpi") }
  }

val srcDir = layout.projectDirectory.dir("src")
val varsDir = layout.projectDirectory.dir("vars")
val resourcesDir = layout.projectDirectory.dir("resources")

val jenkinsWarFile: Provider<File> =
  configurations
    .named(JENKINS_WAR_CONFIGURATION)
    .map { cfg -> cfg.files.single { it.extension == "war" } }

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
val groovyAllRuntime = configurations.register(GROOVY_ALL_RUNTIME_CONFIGURATION) {
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
// artifacts are compiled once and shared across all Jenkins test suites via
// applyJenkinsTestWiring, rather than re-compiled per suite.
val localLibraryRetrieverSourceSet =
  sourceSets.create(LOCAL_LIBRARY_RETRIEVER_SOURCE_SET) {
    java.setSrcDirs(listOf(generateLocalLibraryFiles.flatMap { it.javaOutputDir }))
    resources.setSrcDirs(listOf(generateLocalLibraryFiles.flatMap { it.resourcesOutputDir }))
  }
localLibraryRetrieverSourceSet.groovy.setSrcDirs(emptyList<Any>())
// Jenkins APIs are needed to compile LocalLibraryRetriever / SharedLibraryAutoRegistrar.
configurations.named(localLibraryRetrieverSourceSet.compileOnlyConfigurationName) {
  extendsFrom(jenkinsPlugin)
}
// annotation-indexer processor generates the META-INF index for SharedLibraryAutoRegistrar.
val localLibraryRetrieverAnnotationProcessor =
  configurations.named(localLibraryRetrieverSourceSet.annotationProcessorConfigurationName) {
    extendsFrom(jenkinsBom)
  }
dependencies {
  localLibraryRetrieverAnnotationProcessor(SharedLibraryDefaults.ANNOTATION_INDEXER)
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
        implementation(sharedLibrary.pipelineUnitVersion.map { v -> project.dependencies.create("com.lesfurets:jenkins-pipeline-unit:$v") })
        runtimeOnly(SharedLibraryDefaults.IVY_COORDINATES)
      }
    }

    register<JvmTestSuite>(INTEGRATION_TEST_SUITE) {
      sources {
        java.setSrcDirs(listOf("test/integration/java"))
        groovy.setSrcDirs(listOf("test/integration/groovy"))
        resources.setSrcDirs(listOf("test/integration/resources"))
      }
    }
  }
}

// Wire jenkinsPlugin into each suite's implementation config so variant resolution applies
// rather than raw FileCollection additions that bypass Gradle's dependency management.
// Exclude groovy-all (Groovy 2.4 bundled by jenkins-core): having it on the compile
// classpath alongside groovy:3.x (from Spock 2.x) causes the Groovy compiler to pick up
// the 2.4 runtime, breaking Spock compilation entirely.
testing.suites.withType<JvmTestSuite>().configureEach {
  val implConfigName = sources.implementationConfigurationName
  project.configurations.named(implConfigName) {
    extendsFrom(jenkinsPlugin)
    exclude(mapOf("group" to "org.codehaus.groovy", "module" to "groovy-all"))
  }
}

// ── Jenkins test-harness wiring ───────────────────────────────────────────────

// Applies full Jenkins integration-test wiring to a JvmTestSuite.
// Uses suite.dependencies { } (JvmComponentDependencies) so dependency registration is lazy
// and safe to call at any point during configuration — no afterEvaluate needed.
fun applyJenkinsTestWiring(suite: JvmTestSuite) {
  val suiteName = suite.name
  suite.dependencies {
    implementation("org.jenkins-ci.main:jenkins-test-harness:${SharedLibraryDefaults.TEST_HARNESS_VERSION}")
    // Provide compiled LocalLibraryRetriever + SharedLibraryAutoRegistrar + annotation index.
    implementation(localLibraryRetrieverSourceSet.output)
    // ivy provides @Grab / Grape support inside the embedded Jenkins runtime.
    runtimeOnly(SharedLibraryDefaults.IVY_COORDINATES)
  }
  // Each suite gets its own subdirectory so multiple suites can run in parallel without
  // conflicting on WarExploder output or Gradle's task output tracking.
  val suiteJenkinsDir = layout.buildDirectory.dir("jenkins-for-test/$suiteName")
  suite.targets.configureEach {
    testTask.configure {
      mustRunAfter(tasks.test)
      // WarExploder reads buildDirectory (defaults to "target") as parent of its explode dir.
      jvmArgumentProviders.add(
        objects.newInstance<BuildDirJvmArgumentProvider>().apply {
          dir = suiteJenkinsDir
        },
      )
      outputs.dir(suiteJenkinsDir)
      classpath += files(hpiFiles)
      // groovy-all:2.4 provides the Groovy runtime to the embedded Jenkins pipeline engine.
      // The plugin excludes groovy-all from all suite implementation configs (to prevent
      // groovy 2.4/3.x classpath conflicts in test code), but CpsFlowDefinition needs
      // groovy.lang.Script and the full Groovy runtime at test-task time. += bypasses
      // version-conflict resolution that would otherwise suppress it when groovy 3.x is present.
      classpath += files(groovyAllRuntime)
      doFirst {
        val names = classpath.map { it.name }
        val hasGroovyAll2x = names.any { it.startsWith("groovy-all-2.") }
        val hasGroovy3xPlus = names.any { name -> name.matches(Regex("groovy(-all)?-[34]\\..*\\.jar")) }
        if (hasGroovyAll2x && hasGroovy3xPlus) {
          logger.warn(
            "Jenkins test suite '$suiteName' has both groovy-all:2.x (required for the embedded " +
              "Jenkins pipeline engine) and a Groovy 3.x+ runtime on the classpath. " +
              "CpsFlowDefinition(script, sandbox=true) will fail with a CPS transform error. " +
              "Use sandbox=false for suites that include a Groovy 3.x framework (e.g. Spock 2.x).",
          )
        }
      }
      maxParallelForks = 1
      maxHeapSize = SharedLibraryDefaults.INTEGRATION_TEST_MAX_HEAP_SIZE
      systemProperty("test.library.root", layout.projectDirectory.asFile.absolutePath)
      systemProperty("test.library.src", srcDir.asFile.absolutePath)
      systemProperty("test.library.vars", varsDir.asFile.absolutePath)
      systemProperty("test.library.resources", resourcesDir.asFile.absolutePath)
      systemProperty("test.library.name", sharedLibrary.libraryName.get())
      jvmArgumentProviders.add(
        objects.newInstance<JenkinsWarJvmArgumentProvider>().apply {
          warFile.fileProvider(jenkinsWarFile)
        },
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

testing.suites.named<JvmTestSuite>(INTEGRATION_TEST_SUITE).configure {
  applyJenkinsTestWiring(this)
}

// Consumer suites opt in via sharedLibrary.withJenkins(this) inside their register block.
// applyJenkinsTestWiring uses suite.dependencies { } which is lazy, so it is safe to call
// immediately — no afterEvaluate needed.
sharedLibrary.setJenkinsWirer { suite -> applyJenkinsTestWiring(suite) }

val integrationTestSuiteProvider = testing.suites.named(INTEGRATION_TEST_SUITE)
tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) {
  dependsOn(integrationTestSuiteProvider)
}

// ── Documentation ─────────────────────────────────────────────────────────────

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

// ── Ivy / @Grab support ───────────────────────────────────────────────────────

val ivy =
  configurations.register(IVY_CONFIGURATION) {
    isCanBeResolved = true
    isCanBeConsumed = false
    description = "Ivy for @Grab support in shared library Groovy sources"
  }
dependencies {
  // ivy on the test suite classpath: integration test suites get it via applyJenkinsTestWiring
  // (added to runtimeOnly). The unit test suite gets it via testing.suites.named("test").
  ivy(SharedLibraryDefaults.IVY_COORDINATES)
}
tasks.withType<GroovyCompile>().configureEach {
  groovyClasspath += files(ivy)
}

// ── CodeNarc Enhanced Classpath Rule support ──────────────────────────────────

pluginManager.withPlugin("codenarc") {
  // Enhanced Classpath Rules (rulesets/jenkins.xml) require both the Jenkins
  // dependency JARs AND the compiled .class output of the source being analyzed.
  // Without the .class files on compilationClasspath the rules silently skip.
  // dependsOn(compileGroovy) guarantees the output exists when CodeNarc runs.
  val compileGroovy = tasks.compileGroovy
  tasks.withType<CodeNarc>().configureEach {
    compilationClasspath += files(sourceSets.main.map { it.compileClasspath }, sourceSets.main.map { it.output.classesDirs })
    dependsOn(compileGroovy)
  }

  // Extract the bundled XML to a build-dir file with a .xml extension so CodeNarc
  // parses it as XML rather than as a Groovy DSL script (resources.text.fromUri writes
  // a .txt temp file, which CodeNarc would try to evaluate as Groovy).
  val jenkinsConfigFile = layout.buildDirectory.file("generated/codenarc/codenarc-jenkins.xml")
  val extractJenkinsCodeNarcConfig =
    tasks.register("extractJenkinsCodeNarcConfig") {
      outputs.file(jenkinsConfigFile)
      doLast {
        val path = jenkinsConfigFile.get().asFile.toPath()
        SharedLibraryExtension::class.java.classLoader
          .getResourceAsStream("com/mkobit/jenkins/pipelines/codenarc-jenkins.xml")!!
          .use { input -> path.outputStream().use { out -> input.copyTo(out) } }
      }
    }

  tasks.register<CodeNarc>("codenarcJenkinsMain") {
    description = "Runs Jenkins CPS/Serializable CodeNarc rules against the main source set."
    setSource(sourceSets.main.map { it.groovy })
    dependsOn(extractJenkinsCodeNarcConfig)
    config = resources.text.fromFile(jenkinsConfigFile)
    codenarcClasspath = configurations.getByName("codenarc")
  }

  tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) {
    dependsOn("codenarcJenkinsMain")
  }
}
