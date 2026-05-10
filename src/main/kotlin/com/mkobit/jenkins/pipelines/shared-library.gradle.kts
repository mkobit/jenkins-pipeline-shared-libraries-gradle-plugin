@file:Suppress("ktlint:standard:no-wildcard-imports", "DEPRECATION")

package com.mkobit.jenkins.pipelines

import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.plugins.quality.CodeNarc
import org.gradle.api.tasks.GroovySourceDirectorySet
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.javadoc.Groovydoc
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.testing.base.TestingExtension
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream

plugins {
  groovy
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

val depsHandler = dependencies
val jenkinsPlugin =
  configurations.register(JENKINS_PLUGIN_CONFIGURATION) {
    isCanBeResolved = false
    isCanBeConsumed = false
    description = "Jenkins HPI/JPI plugin dependencies for shared library compilation and testing"
    withDependencies {
      sharedLibrary.jenkins.bomVersion.orNull?.let { bomVer ->
        val (major, minor) =
          sharedLibrary.jenkins.version
            .get()
            .split(".")
        add(depsHandler.platform("io.jenkins.tools.bom:bom-$major.$minor.x:$bomVer"))
      }
    }
  }
dependencies.addProvider(
  JENKINS_PLUGIN_CONFIGURATION,
  sharedLibrary.jenkins.version.map { v -> "org.jenkins-ci.main:jenkins-core:$v" },
)
dependencies.add(JENKINS_PLUGIN_CONFIGURATION, PIPELINE_GROOVY_LIB_MODULE)
dependencies.add(JENKINS_PLUGIN_CONFIGURATION, WORKFLOW_JOB_MODULE)
dependencies.add(JENKINS_PLUGIN_CONFIGURATION, WORKFLOW_BASIC_STEPS_MODULE)
dependencies.add(JENKINS_PLUGIN_CONFIGURATION, WORKFLOW_DURABLE_TASK_STEP_MODULE)

configurations.register(JENKINS_PLUGIN_HPIS_CONFIGURATION) {
  isCanBeResolved = true
  isCanBeConsumed = false
  description = "Jenkins plugin HPI archives for embedded Jenkins runtime (integration tests)"
  extendsFrom(configurations.named(JENKINS_PLUGIN_CONFIGURATION).get())
  attributes {
    attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "hpi")
    attribute(JenkinsPluginRule.JENKINS_ARTIFACT_ATTRIBUTE, "hpi")
  }
}

configurations.register(JENKINS_WAR_CONFIGURATION) {
  isCanBeResolved = true
  isCanBeConsumed = false
  description = "Jenkins WAR file for the embedded Jenkins runtime (integration tests)"
}
dependencies.addProvider(
  JENKINS_WAR_CONFIGURATION,
  sharedLibrary.jenkins.version.map { v -> "org.jenkins-ci.main:jenkins-war:$v@war" },
)

// ── Main source set ───────────────────────────────────────────────────────────

sourceSets.main.apply {
  java.setSrcDirs(emptyList<String>())
  groovy.setSrcDirs(listOf("src", "vars"))
  resources.setSrcDirs(listOf("resources"))
}
// Jenkins APIs are compile-only for the shared library; the library runs inside Jenkins at runtime.
configurations.named("compileOnly") {
  extendsFrom(configurations.named(JENKINS_PLUGIN_CONFIGURATION).get())
}

// ── Test suites ───────────────────────────────────────────────────────────────

// Lenient view so plain-JAR transitives that don't publish HPI are silently skipped
// rather than failing resolution when artifactType=hpi is requested globally.
// JpiCompatibilityRule makes plain JARs compatible with the HPI request; filter to actual
// .hpi/.jpi files so transitive JARs (e.g. groovy-all) don't leak onto the test classpath.
val hpiFiles =
  configurations
    .named(JENKINS_PLUGIN_HPIS_CONFIGURATION)
    .get()
    .incoming
    .artifactView { isLenient = true }
    .artifacts
    .artifactFiles
    .filter { it.name.endsWith(".hpi") || it.name.endsWith(".jpi") }

val projectRoot = layout.projectDirectory.asFile.toPath()
val srcDir = projectRoot.resolve("src").toString()
val varsDir = projectRoot.resolve("vars").toString()
val resourcesDir = projectRoot.resolve("resources").toString()
val libraryRoot = projectRoot.toString()

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
configurations.register(GROOVY_ALL_RUNTIME_CONFIGURATION) {
  isCanBeResolved = true
  isCanBeConsumed = false
}
dependencies.add(
  GROOVY_ALL_RUNTIME_CONFIGURATION,
  SharedLibraryDefaults.GROOVY_ALL_COORDINATES,
)

val generateLocalLibraryFiles =
  tasks.register<GenerateLocalLibraryFiles>("generateLocalLibraryFiles") {
    javaOutputDir.set(layout.buildDirectory.dir("generated-src/localLibraryRetriever/java"))
    resourcesOutputDir.set(layout.buildDirectory.dir("generated-src/localLibraryRetriever/resources"))
    generateAutoRegistrar.set(sharedLibrary.autoRegisterLibrary)
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
configurations.named("${LOCAL_LIBRARY_RETRIEVER_SOURCE_SET}CompileOnly") {
  extendsFrom(jenkinsPlugin.get())
}
// annotation-indexer processor generates the META-INF index for SharedLibraryAutoRegistrar.
dependencies.add("${LOCAL_LIBRARY_RETRIEVER_SOURCE_SET}AnnotationProcessor", SharedLibraryDefaults.ANNOTATION_INDEXER)

extensions.configure<TestingExtension> {
  suites {
    named<JvmTestSuite>("test") {
      useJUnitJupiter()
      sources.apply {
        java.setSrcDirs(listOf("test/unit/java"))
        groovy.setSrcDirs(listOf("test/unit/groovy"))
        resources.setSrcDirs(listOf("test/unit/resources"))
      }
    }

    register<JvmTestSuite>(INTEGRATION_TEST_SUITE) {
      sources.apply {
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
val projectConfigurations = configurations
the<TestingExtension>().suites.withType<JvmTestSuite>().configureEach {
  val implConfigName = sources.implementationConfigurationName
  projectConfigurations.named(implConfigName) {
    extendsFrom(jenkinsPlugin.get())
    exclude(mapOf("group" to "org.codehaus.groovy", "module" to "groovy-all"))
  }
}

// DependencyCollector (used inside suites DSL) has no Provider<String> overload;
// add versioned deps here via DependencyHandler.addProvider which accepts Provider<?>.
dependencies.addProvider(
  "testImplementation",
  sharedLibrary.pipelineUnitVersion.map { v: String -> "com.lesfurets:jenkins-pipeline-unit:$v" },
)

// ── Jenkins test-harness wiring ───────────────────────────────────────────────

// Applies full Jenkins integration-test wiring to a JvmTestSuite:
// - jenkins-test-harness on the implementation classpath
// - ivy on the runtimeOnly classpath (must go through the configuration, not classpath +=,
//   because classpath += before JvmTestSuitePlugin wires the convention would bypass it)
// - HPI plugin archives + groovy-all on the test task classpath
// Called explicitly for the built-in integrationTest suite and exposed via the extension
// so consumers can opt-in their own additional suites (user suites via afterEvaluate below).
fun applyJenkinsTestWiring(suite: JvmTestSuite) {
  val suiteName = suite.name
  val implConfigName = suite.sources.implementationConfigurationName
  depsHandler.add(implConfigName, "org.jenkins-ci.main:jenkins-test-harness:${SharedLibraryDefaults.TEST_HARNESS_VERSION}")
  // Provide compiled LocalLibraryRetriever + SharedLibraryAutoRegistrar + annotation index.
  depsHandler.add(implConfigName, localLibraryRetrieverSourceSet.output)
  // ivy goes through runtimeOnly so it is part of the suite's runtimeClasspath that
  // JvmTestSuitePlugin maps as the test task classpath convention. Adding it via
  // tasks.withType<Test>().configureEach { classpath += ivy } would race against the
  // convention registration for late-registered suites and bypass the runtime classpath.
  depsHandler.add(suite.sources.runtimeOnlyConfigurationName, SharedLibraryDefaults.IVY_COORDINATES)
  // Each suite gets its own subdirectory so multiple suites can run in parallel without
  // conflicting on WarExploder output or Gradle's task output tracking.
  val suiteJenkinsDir = layout.buildDirectory.dir("jenkins-for-test/$suiteName")
  suite.targets.configureEach {
    testTask.configure {
      mustRunAfter(tasks.test)
      // WarExploder reads buildDirectory (defaults to "target") as parent of its explode dir.
      jvmArgumentProviders.add(
        objects.newInstance<BuildDirJvmArgumentProvider>().also {
          it.dir.set(suiteJenkinsDir)
        },
      )
      outputs.dir(suiteJenkinsDir)
      classpath += hpiFiles
      // groovy-all:2.4 provides the Groovy runtime to the embedded Jenkins pipeline engine.
      // The plugin excludes groovy-all from all suite implementation configs (to prevent
      // groovy 2.4/3.x classpath conflicts in test code), but CpsFlowDefinition needs
      // groovy.lang.Script and the full Groovy runtime at test-task time. += bypasses
      // version-conflict resolution that would otherwise suppress it when groovy 3.x is present.
      classpath += configurations.getByName(GROOVY_ALL_RUNTIME_CONFIGURATION)
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
      systemProperty("test.library.root", libraryRoot)
      systemProperty("test.library.src", srcDir)
      systemProperty("test.library.vars", varsDir)
      systemProperty("test.library.resources", resourcesDir)
      systemProperty("test.library.name", sharedLibrary.libraryName.get())
      jvmArgumentProviders.add(
        objects.newInstance<JenkinsWarJvmArgumentProvider>().also {
          it.warFile.fileProvider(jenkinsWarFile)
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

val integrationTestSuite = the<TestingExtension>().suites.named<JvmTestSuite>(INTEGRATION_TEST_SUITE)
applyJenkinsTestWiring(integrationTestSuite.get())

// Consumer-registered suites opt in via sharedLibrary.useJenkinsTestRunnerSuite(suite).
// Those calls arrive during the consumer's build-script evaluation — before the suite
// is added to the container and before JvmTestSuitePlugin's suites.all {} hook sets up
// the Test.classpath convention. Deferring to afterEvaluate ensures the convention is
// already in place when applyJenkinsTestWiring appends hpiFiles to the test classpath.
val deferredUserSuites = mutableListOf<JvmTestSuite>()
afterEvaluate {
  deferredUserSuites.forEach { applyJenkinsTestWiring(it) }
}
sharedLibrary.setTestSuiteWirer { suite -> deferredUserSuites.add(suite) }

tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) {
  dependsOn(integrationTestSuite)
}

// ── Documentation ─────────────────────────────────────────────────────────────

tasks {
  register<Jar>("sourcesJar") {
    description = "Assembles a JAR of the source"
    archiveClassifier.set("sources")
    from(sourceSets.main.allSource)
  }
  val groovydoc = named<Groovydoc>(GroovyPlugin.GROOVYDOC_TASK_NAME)
  register<Jar>("groovydocJar") {
    description = "Assembles the Groovydoc JAR"
    archiveClassifier.set("javadoc")
    from(groovydoc.map { it.destinationDir })
  }
}

// ── Ivy / @Grab support ───────────────────────────────────────────────────────

configurations.register(IVY_CONFIGURATION) {
  isCanBeResolved = true
  isCanBeConsumed = false
  description = "Ivy for @Grab support in shared library Groovy sources"
}
dependencies.add(IVY_CONFIGURATION, SharedLibraryDefaults.IVY_COORDINATES)
tasks.withType<GroovyCompile>().configureEach {
  groovyClasspath += configurations.getByName(IVY_CONFIGURATION)
}
// ivy on the test suite classpath: integration test suites get it via applyJenkinsTestWiring
// (added to runtimeOnly). The unit test suite gets it here directly.
dependencies.add("testRuntimeOnly", SharedLibraryDefaults.IVY_COORDINATES)

// ── CodeNarc Enhanced Classpath Rule support ──────────────────────────────────

pluginManager.withPlugin("codenarc") {
  val mainCompileClasspath = sourceSets.main.compileClasspath
  // Enhanced Classpath Rules (rulesets/jenkins.xml) require both the Jenkins
  // dependency JARs AND the compiled .class output of the source being analyzed.
  // Without the .class files on compilationClasspath the rules silently skip.
  // dependsOn(compileGroovy) guarantees the output exists when CodeNarc runs.
  val mainClassesDirs = sourceSets.main.output.classesDirs
  val compileGroovy = tasks.compileGroovy
  tasks.withType<CodeNarc>().configureEach {
    compilationClasspath += mainCompileClasspath
    compilationClasspath += mainClassesDirs
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
        path.parent.createDirectories()
        SharedLibraryExtension::class.java.classLoader
          .getResourceAsStream("com/mkobit/jenkins/pipelines/codenarc-jenkins.xml")!!
          .use { input -> path.outputStream().use { out -> input.copyTo(out) } }
      }
    }

  tasks.register<CodeNarc>("codenarcJenkinsMain") {
    description = "Runs Jenkins CPS/Serializable CodeNarc rules against the main source set."
    source = sourceSets.main.groovy
    dependsOn(extractJenkinsCodeNarcConfig)
    config = resources.text.fromFile(jenkinsConfigFile)
    codenarcClasspath = configurations.getByName("codenarc")
    reports {
      text.required.set(true)
      xml.required.set(false)
      html.required.set(false)
      text.outputLocation.set(layout.buildDirectory.file("reports/codenarc/jenkinsMain.txt"))
    }
  }

  tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) {
    dependsOn("codenarcJenkinsMain")
  }
}

// ── Type-safe source set accessors ────────────────────────────────────────────

private val Project.sourceSets: SourceSetContainer
  get() = the()

private val SourceSetContainer.main: SourceSet
  get() = getByName("main")

private val SourceSet.groovy: org.gradle.api.file.SourceDirectorySet
  get() = extensions.getByType<GroovySourceDirectorySet>()
