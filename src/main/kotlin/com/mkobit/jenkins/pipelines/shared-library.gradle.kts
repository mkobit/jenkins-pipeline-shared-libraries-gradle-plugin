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
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.testing.base.TestingExtension

plugins {
  groovy
}

// ── Extension ─────────────────────────────────────────────────────────────────

val ext = extensions.create("sharedLibrary", SharedLibraryExtension::class.java)
ext.jenkins.version.convention(SharedLibraryDefaults.CORE_VERSION)
ext.jenkins.testHarnessVersion.convention(SharedLibraryDefaults.TEST_HARNESS_VERSION)
ext.jenkins.bomVersion.convention(SharedLibraryDefaults.BOM_VERSION)
ext.pipelineUnitVersion.convention(SharedLibraryDefaults.PIPELINE_UNIT_VERSION)
ext.autoRegisterLibrary.convention(true)
ext.libraryName.convention(project.name)

// ── Jenkins plugin configurations ─────────────────────────────────────────────

dependencies.components.all(JenkinsPluginRule::class.java)
dependencies.components.withModule(
  "org.jenkins-ci.main:jenkins-test-harness",
  JenkinsTestHarnessServletApiRule::class.java,
)

dependencies.attributesSchema.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE) {
  compatibilityRules.add(JpiCompatibilityRule::class.java)
}
dependencies.attributesSchema.attribute(JenkinsPluginRule.JENKINS_ARTIFACT_ATTRIBUTE) {
  disambiguationRules.add(JenkinsArtifactDisambiguationRule::class.java)
}

// Eagerly created so the Kotlin DSL generates the jenkinsPlugin(...) typed accessor at sync time.
val depsHandler = dependencies
configurations.create(PluginConstants.JENKINS_PLUGIN_CONFIGURATION) {
  isCanBeResolved = false
  isCanBeConsumed = false
  description = "Jenkins HPI/JPI plugin dependencies for shared library compilation and testing"
  withDependencies {
    ext.jenkins.bomVersion.orNull?.let { bomVer ->
      val (major, minor) =
        ext.jenkins.version
          .get()
          .split(".")
      add(depsHandler.platform("io.jenkins.tools.bom:bom-$major.$minor.x:$bomVer"))
    }
  }
}
dependencies.addProvider(
  PluginConstants.JENKINS_PLUGIN_CONFIGURATION,
  ext.jenkins.version.map { v -> "org.jenkins-ci.main:jenkins-core:$v" },
)
dependencies.add(PluginConstants.JENKINS_PLUGIN_CONFIGURATION, PluginConstants.DEFAULT_PIPELINE_GROOVY_LIB)
dependencies.add(PluginConstants.JENKINS_PLUGIN_CONFIGURATION, PluginConstants.DEFAULT_WORKFLOW_JOB)
dependencies.add(PluginConstants.JENKINS_PLUGIN_CONFIGURATION, PluginConstants.DEFAULT_WORKFLOW_BASIC_STEPS)
dependencies.add(PluginConstants.JENKINS_PLUGIN_CONFIGURATION, PluginConstants.DEFAULT_WORKFLOW_DURABLE_TASK_STEP)

configurations.create(PluginConstants.JENKINS_PLUGIN_HPIS_CONFIGURATION) {
  isCanBeResolved = true
  isCanBeConsumed = false
  description = "Jenkins plugin HPI archives for embedded Jenkins runtime (integration tests)"
  extendsFrom(configurations.getByName(PluginConstants.JENKINS_PLUGIN_CONFIGURATION))
  attributes {
    attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "hpi")
    attribute(JenkinsPluginRule.JENKINS_ARTIFACT_ATTRIBUTE, "hpi")
  }
}

configurations.create(PluginConstants.JENKINS_WAR_CONFIGURATION) {
  isCanBeResolved = true
  isCanBeConsumed = false
  description = "Jenkins WAR file for the embedded Jenkins runtime (integration tests)"
}
dependencies.addProvider(
  PluginConstants.JENKINS_WAR_CONFIGURATION,
  ext.jenkins.version.map { v -> "org.jenkins-ci.main:jenkins-war:$v@war" },
)

// ── Main source set ───────────────────────────────────────────────────────────

sourceSets.main.apply {
  java.setSrcDirs(emptyList<String>())
  groovy.setSrcDirs(listOf("src", "vars"))
  resources.setSrcDirs(listOf("resources"))
}
// Jenkins APIs are compile-only for the shared library; the library runs inside Jenkins at runtime.
configurations.named("compileOnly") {
  extendsFrom(configurations.getByName(PluginConstants.JENKINS_PLUGIN_CONFIGURATION))
}

// ── Test suites ───────────────────────────────────────────────────────────────

val jenkinsPlugin = configurations.getByName(PluginConstants.JENKINS_PLUGIN_CONFIGURATION)

// Lenient view so plain-JAR transitives that don't publish HPI are silently skipped
// rather than failing resolution when artifactType=hpi is requested globally.
// JpiCompatibilityRule makes plain JARs compatible with the HPI request; filter to actual
// .hpi/.jpi files so transitive JARs (e.g. groovy-all) don't leak onto the test classpath.
val hpiFiles =
  configurations
    .getByName(PluginConstants.JENKINS_PLUGIN_HPIS_CONFIGURATION)
    .incoming
    .artifactView { isLenient = true }
    .artifacts
    .artifactFiles
    .filter { it.name.endsWith(".hpi") || it.name.endsWith(".jpi") }

val srcDir =
  layout.projectDirectory
    .dir("src")
    .asFile.absolutePath
val varsDir =
  layout.projectDirectory
    .dir("vars")
    .asFile.absolutePath
val resourcesDir =
  layout.projectDirectory
    .dir("resources")
    .asFile.absolutePath
val libraryRoot = layout.projectDirectory.asFile.absolutePath

val jenkinsWarFile: Provider<File> =
  configurations
    .named(PluginConstants.JENKINS_WAR_CONFIGURATION)
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
val groovyAllRuntime =
  configurations.create(PluginConstants.GROOVY_ALL_RUNTIME_CONFIGURATION) {
    isCanBeResolved = true
    isCanBeConsumed = false
  }
dependencies.add(
  PluginConstants.GROOVY_ALL_RUNTIME_CONFIGURATION,
  "${PluginConstants.GROOVY_ALL_GROUP_AND_ARTIFACT}:${SharedLibraryDefaults.GROOVY_ALL_VERSION}",
)

val generateLocalLibraryFiles =
  tasks.register<GenerateLocalLibraryFiles>("generateLocalLibraryFiles") {
    javaOutputDir.set(layout.buildDirectory.dir("generated-src/integrationTest/java"))
    resourcesOutputDir.set(layout.buildDirectory.dir("generated-src/integrationTest/resources"))
    generateAutoRegistrar.set(ext.autoRegisterLibrary)
  }

extensions.configure<TestingExtension> {
  suites {
    val test by getting(JvmTestSuite::class) {
      useJUnitJupiter()
      sources.apply {
        java.setSrcDirs(listOf("test/unit/java"))
        groovy.setSrcDirs(listOf("test/unit/groovy"))
        resources.setSrcDirs(listOf("test/unit/resources"))
      }
    }

    val integrationTest by registering(JvmTestSuite::class) {
      sources.apply {
        java.setSrcDirs(
          listOf("test/integration/java", generateLocalLibraryFiles.flatMap { it.javaOutputDir }),
        )
        groovy.setSrcDirs(listOf("test/integration/groovy"))
        resources.setSrcDirs(
          listOf(
            "test/integration/resources",
            generateLocalLibraryFiles.flatMap { it.resourcesOutputDir },
          ),
        )
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
    extendsFrom(jenkinsPlugin)
    exclude(mapOf("group" to "org.codehaus.groovy", "module" to "groovy-all"))
  }
}

// DependencyCollector (used inside suites DSL) has no Provider<String> overload;
// add versioned deps here via DependencyHandler.addProvider which accepts Provider<?>.
dependencies.addProvider(
  "testImplementation",
  ext.pipelineUnitVersion.map { v: String -> "com.lesfurets:jenkins-pipeline-unit:$v" },
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
  val implConfigName = suite.sources.implementationConfigurationName
  depsHandler.addProvider(
    implConfigName,
    ext.jenkins.testHarnessVersion.map { v: String -> "org.jenkins-ci.main:jenkins-test-harness:$v" },
  )
  // ivy goes through runtimeOnly so it is part of the suite's runtimeClasspath that
  // JvmTestSuitePlugin maps as the test task classpath convention. Adding it via
  // tasks.withType<Test>().configureEach { classpath += ivy } would race against the
  // convention registration for late-registered suites and bypass the runtime classpath.
  depsHandler.add(suite.sources.runtimeOnlyConfigurationName, PluginConstants.IVY_COORDINATES)
  // Each suite gets its own subdirectory so multiple suites can run in parallel without
  // conflicting on WarExploder output or Gradle's task output tracking.
  val suiteJenkinsDir = layout.buildDirectory.dir("jenkins-for-test/${suite.name}")
  // buildDirectory is finalized at configuration time; .get() is safe here.
  val suiteBuildDir = suiteJenkinsDir.get().asFile.absolutePath
  suite.targets.all {
    testTask.configure {
      mustRunAfter(tasks.named("test"))
      // WarExploder reads buildDirectory (defaults to "target") as parent of its explode dir.
      systemProperty("buildDirectory", suiteBuildDir)
      outputs.dir(suiteJenkinsDir)
      classpath += hpiFiles
      // groovyAllRuntime is an isolated configuration that forces groovy-all 2.4 onto the
      // classpath even when groovy 3.x is present via Spock. Must use += (not runtimeOnly)
      // to bypass version-conflict resolution that would otherwise pick groovy 3.x.
      classpath += groovyAllRuntime
      maxParallelForks = 1
      maxHeapSize = SharedLibraryDefaults.INTEGRATION_TEST_MAX_HEAP_SIZE
      systemProperty("test.library.root", libraryRoot)
      systemProperty("test.library.src", srcDir)
      systemProperty("test.library.vars", varsDir)
      systemProperty("test.library.resources", resourcesDir)
      systemProperty("test.library.name", ext.libraryName.get())
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

val integrationTestSuite = the<TestingExtension>().suites.named<JvmTestSuite>(PluginConstants.INTEGRATION_TEST_SUITE)
applyJenkinsTestWiring(integrationTestSuite.get())

// annotation-indexer processor indexes @Initializer on SharedLibraryAutoRegistrar so
// Jenkins' InitializerFinder (via Index.list) can discover and call it at embedded
// Jenkins startup time. Uses org.jvnet.hudson.annotation_indexer.Indexed meta-annotation
// which generates META-INF/services/annotations/hudson.init.Initializer (simple text
// format, class names only — not SezPoz binary format).
// Must be added after applyJenkinsTestWiring forces the integrationTest suite to realize,
// which is when JvmTestSuitePlugin creates the integrationTestAnnotationProcessor config.
// Always on the processor path; generateLocalLibraryFiles controls whether the annotated
// source file is generated based on autoRegisterLibrary.
dependencies.add("integrationTestAnnotationProcessor", PluginConstants.ANNOTATION_INDEXER_COORDINATES)

// Consumer-registered suites opt in via sharedLibrary.jenkinsTestRunnerSuite(suite).
// Those calls arrive during the consumer's build-script evaluation — before the suite
// is added to the container and before JvmTestSuitePlugin's suites.all {} hook sets up
// the Test.classpath convention. Deferring to afterEvaluate ensures the convention is
// already in place when applyJenkinsTestWiring appends hpiFiles to the test classpath.
val deferredUserSuites = mutableListOf<JvmTestSuite>()
afterEvaluate {
  deferredUserSuites.forEach { applyJenkinsTestWiring(it) }
}
ext.setTestSuiteWirer { suite -> deferredUserSuites.add(suite) }

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

val ivy =
  configurations.create(PluginConstants.IVY_CONFIGURATION) {
    isCanBeResolved = true
    isCanBeConsumed = false
    description = "Ivy for @Grab support in shared library Groovy sources"
  }
dependencies.add(ivy.name, PluginConstants.IVY_COORDINATES)
tasks.withType<GroovyCompile>().configureEach {
  groovyClasspath += ivy
}
// ivy on the test suite classpath: integration test suites get it via applyJenkinsTestWiring
// (added to runtimeOnly). The unit test suite gets it here directly.
dependencies.add("testRuntimeOnly", PluginConstants.IVY_COORDINATES)

// ── CodeNarc Enhanced Classpath Rule support ──────────────────────────────────

pluginManager.withPlugin("codenarc") {
  val mainCompileClasspath = sourceSets.main.compileClasspath
  // Enhanced Classpath Rules (rulesets/jenkins.xml) require both the Jenkins
  // dependency JARs AND the compiled .class output of the source being analyzed.
  // Without the .class files on compilationClasspath the rules silently skip.
  // dependsOn(compileGroovy) guarantees the output exists when CodeNarc runs.
  val mainClassesDirs = sourceSets.main.output.classesDirs
  val compileGroovy = tasks.named("compileGroovy")
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
        val file = jenkinsConfigFile.get().asFile
        file.parentFile.mkdirs()
        SharedLibraryExtension::class.java.classLoader
          .getResourceAsStream("com/mkobit/jenkins/pipelines/codenarc-jenkins.xml")!!
          .use { input -> file.outputStream().use { out -> input.copyTo(out) } }
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
  get() = extensions.getByType(GroovySourceDirectorySet::class.java)
