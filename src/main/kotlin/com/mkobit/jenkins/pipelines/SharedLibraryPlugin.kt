@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.mkobit.jenkins.pipelines

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.file.ProjectLayout
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.tasks.GroovySourceDirectorySet
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.javadoc.Groovydoc
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.testing.base.TestingExtension
import javax.inject.Inject

class SharedLibraryPlugin
  @Inject
  constructor(
    private val projectLayout: ProjectLayout,
  ) : Plugin<Project> {
    companion object {
      private const val DEFAULT_JENKINS_PIPELINE_UNIT_VERSION = "1.29"
      internal const val DEFAULT_CORE_VERSION = "2.479.1"
      internal const val DEFAULT_TEST_HARNESS_VERSION = "2391.v9b_3e2d3351a_2"

      private const val INTEGRATION_TEST_SUITE = "integrationTest"

      private const val JENKINS_PLUGIN_CONFIGURATION = "jenkinsPlugin"
      private const val JENKINS_PLUGIN_HPIS_CONFIGURATION = "jenkinsPluginHpis"
      private const val JENKINS_WAR_CONFIGURATION = "jenkinsWar"

      // Default pipeline runtime added to jenkinsPlugin automatically.
      // Version-free: the user's Jenkins BOM manages them. pipeline-groovy-lib
      // provides the library loading engine; its transitive required dependencies
      // (workflow-cps, workflow-api, script-security, etc.) are pulled in as HPIs.
      // workflow-job, workflow-basic-steps, and workflow-durable-task-step cover
      // the minimum step surface (node, echo, sh) for JenkinsRule integration tests.
      // Users who need additional plugins (pipeline-model-definition, lockable-resources,
      // etc.) declare them via jenkinsPlugin(...) in their build script.
      private const val DEFAULT_PIPELINE_GROOVY_LIB = "io.jenkins.plugins:pipeline-groovy-lib"
      private const val DEFAULT_WORKFLOW_JOB = "org.jenkins-ci.plugins.workflow:workflow-job"
      private const val DEFAULT_WORKFLOW_BASIC_STEPS = "org.jenkins-ci.plugins.workflow:workflow-basic-steps"
      private const val DEFAULT_WORKFLOW_DURABLE_TASK_STEP = "org.jenkins-ci.plugins.workflow:workflow-durable-task-step"

      private const val IVY_CONFIGURATION = "sharedLibraryIvy"
      private const val IVY_COORDINATES = "org.apache.ivy:ivy:2.4.0"

      // groovy-all:2.4.x ships with Jenkins core and contains DGM classes (SqlGroovyMethods etc.)
      // referenced by script-security's SandboxInterceptor static initializer. These classes
      // were removed in Groovy 3.x so they must be supplied explicitly for integration tests.
      private const val GROOVY_ALL_RUNTIME_CONFIGURATION = "integrationTestGroovyAllRuntime"
      private const val GROOVY_ALL_COORDINATES = "org.codehaus.groovy:groovy-all:2.4.21"
    }

    override fun apply(target: Project) {
      target.run {
        val ext = extensions.create("sharedLibrary", SharedLibraryExtension::class.java)
        ext.jenkins.version.convention(DEFAULT_CORE_VERSION)
        ext.jenkins.testHarnessVersion.convention(DEFAULT_TEST_HARNESS_VERSION)
        ext.pipelineUnitVersion.convention(DEFAULT_JENKINS_PIPELINE_UNIT_VERSION)

        apply<GroovyPlugin>()
        setupJenkinsPluginConfiguration(ext)
        setupMain()
        setupTestSuites(ext)
        setupDocumentationTasks()
        setupIvyGrabSupport()
      }
    }

    @Suppress("DEPRECATION")
    private fun Project.setupJenkinsPluginConfiguration(ext: SharedLibraryExtension) {
      dependencies.components.all(JenkinsPluginRule::class.java)

      dependencies.attributesSchema.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE) {
        compatibilityRules.add(JpiCompatibilityRule::class.java)
      }
      dependencies.attributesSchema.attribute(JenkinsPluginRule.JENKINS_ARTIFACT_ATTRIBUTE) {
        disambiguationRules.add(JenkinsArtifactDisambiguationRule::class.java)
      }

      // Eagerly created so the Kotlin DSL generates the jenkinsPlugin(...) typed accessor at sync time.
      configurations.create(JENKINS_PLUGIN_CONFIGURATION) {
        isCanBeResolved = false
        isCanBeConsumed = false
        description = "Jenkins HPI/JPI plugin dependencies for shared library compilation and testing"
      }
      dependencies.addProvider(JENKINS_PLUGIN_CONFIGURATION, ext.jenkins.version.map { v -> "org.jenkins-ci.main:jenkins-core:$v" })
      dependencies.add(JENKINS_PLUGIN_CONFIGURATION, DEFAULT_PIPELINE_GROOVY_LIB)
      dependencies.add(JENKINS_PLUGIN_CONFIGURATION, DEFAULT_WORKFLOW_JOB)
      dependencies.add(JENKINS_PLUGIN_CONFIGURATION, DEFAULT_WORKFLOW_BASIC_STEPS)
      dependencies.add(JENKINS_PLUGIN_CONFIGURATION, DEFAULT_WORKFLOW_DURABLE_TASK_STEP)

      // Internal: raw HPI archives for the embedded Jenkins runtime used by JenkinsRule.
      configurations.create(JENKINS_PLUGIN_HPIS_CONFIGURATION) {
        isCanBeResolved = true
        isCanBeConsumed = false
        description = "Jenkins plugin HPI archives for embedded Jenkins runtime (integration tests)"
        extendsFrom(configurations.getByName(JENKINS_PLUGIN_CONFIGURATION))
        attributes {
          attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "hpi")
          attribute(JenkinsPluginRule.JENKINS_ARTIFACT_ATTRIBUTE, "hpi")
        }
      }

      // Internal: Jenkins WAR for WarExploder (JenkinsRule). Resolved lazily at test execution.
      configurations.create(JENKINS_WAR_CONFIGURATION) {
        isCanBeResolved = true
        isCanBeConsumed = false
        description = "Jenkins WAR file for the embedded Jenkins runtime (integration tests)"
      }
      dependencies.addProvider(JENKINS_WAR_CONFIGURATION, ext.jenkins.version.map { v -> "org.jenkins-ci.main:jenkins-war:$v@war" })
    }

    private fun Project.setupMain() {
      sourceSets.main.apply {
        java.setSrcDirs(emptyList<String>())
        groovy.setSrcDirs(listOf("src", "vars"))
        resources.setSrcDirs(listOf("resources"))
      }
      // Jenkins APIs are compile-only for the shared library; the library runs inside Jenkins at runtime.
      configurations.named("compileOnly") {
        extendsFrom(configurations.getByName(JENKINS_PLUGIN_CONFIGURATION))
      }
    }

    private fun Project.setupTestSuites(ext: SharedLibraryExtension) {
      val jenkinsPlugin = configurations.getByName(JENKINS_PLUGIN_CONFIGURATION)

      // Lenient view so plain-JAR transitives that don't publish HPI are silently skipped
      // rather than failing resolution when artifactType=hpi is requested globally.
      // JpiCompatibilityRule makes plain JARs compatible with the HPI request; filter to actual
      // .hpi/.jpi files so transitive JARs (e.g. groovy-all) don't leak onto the test classpath.
      val hpiFiles =
        configurations
          .getByName(JENKINS_PLUGIN_HPIS_CONFIGURATION)
          .incoming
          .artifactView { isLenient = true }
          .artifacts
          .artifactFiles
          .filter { it.name.endsWith(".hpi") || it.name.endsWith(".jpi") }

      val srcDir =
        projectLayout.projectDirectory
          .dir("src")
          .asFile.absolutePath
      val varsDir =
        projectLayout.projectDirectory
          .dir("vars")
          .asFile.absolutePath
      val resourcesDir =
        projectLayout.projectDirectory
          .dir("resources")
          .asFile.absolutePath
      val libraryRoot = projectLayout.projectDirectory.asFile.absolutePath

      val jenkinsWar = configurations.getByName(JENKINS_WAR_CONFIGURATION)

      // Integration tests need groovy-all at *runtime only* so SandboxInterceptor
      // (script-security plugin) can load SqlGroovyMethods and other Groovy 2.4 DGM classes
      // that no longer exist in the groovy-3.x module jars. An isolated configuration bypasses
      // the groovy-all exclusion on implementation, keeping it off the compile classpath.
      val groovyAllRuntime =
        configurations.create(GROOVY_ALL_RUNTIME_CONFIGURATION) {
          isCanBeResolved = true
          isCanBeConsumed = false
        }
      dependencies.add(GROOVY_ALL_RUNTIME_CONFIGURATION, GROOVY_ALL_COORDINATES)

      val generateLocalLibraryFiles =
        tasks.register<GenerateLocalLibraryFiles>("generateLocalLibraryFiles") {
          javaOutputDir.set(layout.buildDirectory.dir("generated-src/integrationTest/java"))
          resourcesOutputDir.set(layout.buildDirectory.dir("generated-src/integrationTest/resources"))
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
            targets.all {
              testTask.configure {
                mustRunAfter(test)
                description = "Runs integration tests against an embedded Jenkins runtime"
                // WarExploder hardcodes ./target/jenkins-for-test as the WAR explode directory.
                // Setting workingDir to the build directory redirects that path inside build/,
                // which is already gitignored, keeping the project root clean.
                workingDir = layout.buildDirectory.asFile.get()
                classpath += hpiFiles
                classpath += groovyAllRuntime
                maxParallelForks = 1
                maxHeapSize = "2g"
                systemProperty("test.library.root", libraryRoot)
                systemProperty("test.library.src", srcDir)
                systemProperty("test.library.vars", varsDir)
                systemProperty("test.library.resources", resourcesDir)
                jvmArgumentProviders.add(
                  CommandLineArgumentProvider {
                    listOf("-Djth.jenkins-war.path=${jenkinsWar.files.single { it.extension == "war" }.absolutePath}")
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
        }
      }

      // Wire jenkinsPlugin into each suite's implementation config so variant resolution applies
      // rather than raw FileCollection additions that bypass Gradle's dependency management.
      // Exclude groovy-all (Groovy 2.4 bundled by jenkins-core): having it on the compile
      // classpath alongside groovy:3.x (from Spock 2.x) causes the Groovy compiler to pick up
      // the 2.4 runtime, breaking Spock compilation entirely.
      the<TestingExtension>().suites.withType<JvmTestSuite>().configureEach {
        val implConfigName = sources.implementationConfigurationName
        this@setupTestSuites.configurations.named(implConfigName) {
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
      dependencies.addProvider(
        "${INTEGRATION_TEST_SUITE}Implementation",
        ext.jenkins.testHarnessVersion.map { v: String -> "org.jenkins-ci.main:jenkins-test-harness:$v" },
      )

      val integrationTestSuite = the<TestingExtension>().suites.named(INTEGRATION_TEST_SUITE)
      tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) {
        dependsOn(integrationTestSuite)
      }
    }

    @Suppress("DEPRECATION")
    private fun Project.setupIvyGrabSupport() {
      val ivy =
        configurations.create(IVY_CONFIGURATION) {
          isCanBeResolved = true
          isCanBeConsumed = false
          description = "Ivy for @Grab support in shared library Groovy sources"
        }
      dependencies.add(ivy.name, IVY_COORDINATES)
      tasks {
        withType<GroovyCompile>().configureEach {
          groovyClasspath += ivy
        }
        withType<Test>().configureEach {
          classpath += ivy
        }
      }
    }

    private fun Project.setupDocumentationTasks() {
      tasks {
        register<Jar>("sourcesJar") {
          description = "Assembles a JAR of the source"
          archiveClassifier.set("sources")
          from(sourceSets.main.allSource)
        }
        val groovydoc = named<Groovydoc>(GroovyPlugin.GROOVYDOC_TASK_NAME)
        register<Jar>("groovydocJar") {
          description = "Assembles the Groovydoc JAR"
          dependsOn(groovydoc)
          archiveClassifier.set("javadoc")
        }
      }
    }

    private val Project.sourceSets: SourceSetContainer
      get() = the()

    private val SourceSetContainer.main: SourceSet
      get() = getByName("main")

    private val SourceSet.groovy: org.gradle.api.file.SourceDirectorySet
      get() = extensions.getByType(GroovySourceDirectorySet::class.java)
  }
