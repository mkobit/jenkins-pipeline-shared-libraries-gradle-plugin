@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.mkobit.jenkins.pipelines

import io.github.oshai.kotlinlogging.KotlinLogging
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.tasks.GroovySourceDirectorySet
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.javadoc.Groovydoc
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.language.base.plugins.LifecycleBasePlugin
import javax.inject.Inject

open class SharedLibraryPlugin
  @Inject
  constructor(
    private val projectLayout: ProjectLayout,
    private val objectFactory: ObjectFactory,
  ) : Plugin<Project> {
    companion object {
      private val LOGGER = KotlinLogging.logger {}
      private const val SHARED_LIBRARY_EXTENSION_NAME = "sharedLibrary"
      private const val TEST_ROOT_PATH = "test"
      private const val DEFAULT_JENKINS_PIPELINE_UNIT_VERSION = "1.29"
      private const val DEFAULT_TEST_HARNESS_VERSION = "2.50"
      private const val DEFAULT_CORE_VERSION = "2.479.1"

      private const val INTEGRATION_TEST_SOURCE_SET = "integrationTest"
      private const val INTEGRATION_TEST_TASK = "integrationTest"

      private const val JENKINS_PLUGIN_CONFIGURATION = "jenkinsPlugin"
      private const val JENKINS_PLUGIN_CLASSPATH_CONFIGURATION = "jenkinsPluginClasspath"
      private const val JENKINS_PLUGIN_HPIS_CONFIGURATION = "jenkinsPluginHpis"
      private const val UNIT_TESTING_LIBRARY_CONFIGURATION = "jenkinsPipelineUnit"
      private const val TEST_HARNESS_CONFIGURATION = "jenkinsTestHarness"

      private const val IVY_CONFIGURATION = "sharedLibraryIvy"
      private const val IVY_COORDINATES = "org.apache.ivy:ivy:2.4.0"
    }

    override fun apply(target: Project) {
      target.run {
        apply<GroovyPlugin>()
        apply<JenkinsIntegrationPlugin>()
        setupSharedLibraryExtension()
        setupJenkinsPluginConfiguration()
        setupMain()
        setupUnitTest()
        setupIntegrationTest()
        setupDocumentationTasks()
        setupIvyGrabSupport()
      }
    }

    private fun Project.setupJenkinsPluginConfiguration() {
      // CMR stamps artifactType on the compile/runtime variants of every Jenkins plugin
      // component so that artifact-type-aware resolution selects the right published file
      // (JAR for compilation, HPI for the embedded Jenkins runtime) without any extraction.
      dependencies.components.all(JenkinsPluginRule::class.java)

      val dependencyHandler = dependencies

      // User-facing bucket — consumers declare jenkinsPlugin(...) here
      val jenkinsPlugin =
        configurations.create(JENKINS_PLUGIN_CONFIGURATION) {
          isCanBeResolved = false
          isCanBeConsumed = false
          description = "Jenkins HPI/JPI plugin dependencies for shared library compilation and testing"
        }

      // Internal: resolves plugin JARs + jenkins-core (provided scope); wired to compile/test classpaths.
      // jenkins-core is added automatically because plugins declare it as <scope>provided</scope>
      // so Gradle never pulls it transitively — but shared library Groovy compilation needs it.
      // Version is managed by whatever BOM the consumer has declared via jenkinsPlugin(platform(...)).
      configurations.create(JENKINS_PLUGIN_CLASSPATH_CONFIGURATION) {
        isCanBeResolved = true
        isCanBeConsumed = false
        isVisible = false
        description = "Jenkins plugin JARs + jenkins-core for shared library compilation and unit tests"
        extendsFrom(jenkinsPlugin)
        withDependencies {
          dependencyHandler.add(name, "org.jenkins-ci.main:jenkins-core:$DEFAULT_CORE_VERSION")
        }
        attributes {
          attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE)
        }
      }

      // Internal: resolves raw HPI archives; wired to integration test runtime for JenkinsRule
      configurations.create(JENKINS_PLUGIN_HPIS_CONFIGURATION) {
        isCanBeResolved = true
        isCanBeConsumed = false
        isVisible = false
        description = "Jenkins plugin HPI archives for embedded Jenkins runtime (integration tests)"
        extendsFrom(jenkinsPlugin)
        attributes {
          attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "hpi")
        }
      }
    }

    private fun Project.setupMain() {
      sourceSets.main.apply {
        java.setSrcDirs(emptyList<String>())
        groovy.setSrcDirs(listOf("src", "vars"))
        resources.setSrcDirs(listOf("resources"))
        val pluginClasspath = configurations.getByName(JENKINS_PLUGIN_CLASSPATH_CONFIGURATION)
        compileClasspath += pluginClasspath
        runtimeClasspath += pluginClasspath
      }
    }

    private fun Project.setupUnitTest() {
      val dependencyHandler = dependencies
      val test =
        sourceSets.test.apply {
          val unitTestDirectory = "$TEST_ROOT_PATH/unit"
          java.setSrcDirs(listOf("$unitTestDirectory/java"))
          groovy.setSrcDirs(listOf("$unitTestDirectory/groovy"))
          resources.setSrcDirs(listOf("$unitTestDirectory/resources"))
          val pluginClasspath = configurations.getByName(JENKINS_PLUGIN_CLASSPATH_CONFIGURATION)
          compileClasspath += pluginClasspath
          runtimeClasspath += pluginClasspath
        }
      configurations {
        val pipelineUnit =
          create(UNIT_TESTING_LIBRARY_CONFIGURATION) {
            isCanBeResolved = true
            isCanBeConsumed = false
            isVisible = false
            description = "JenkinsPipelineUnit library for shared library unit tests"
            withDependencies {
              LOGGER.debug { "Adding JenkinsPipelineUnit to configuration $name" }
              dependencyHandler.add(name, sharedLibraryExtension.pipelineUnitDependency().get())
            }
          }
        getByName(test.implementationConfigurationName) {
          extendsFrom(pipelineUnit)
        }
      }
    }

    private fun Project.setupIntegrationTest() {
      val dependencyHandler = dependencies
      val integrationTestSourceSet =
        sourceSets.create(INTEGRATION_TEST_SOURCE_SET) {
          description = "Integration test source set for shared library"
          val integrationTestDirectory = "$TEST_ROOT_PATH/integration"
          java.setSrcDirs(listOf("$integrationTestDirectory/java"))
          groovy.setSrcDirs(listOf("$integrationTestDirectory/groovy"))
          resources.setSrcDirs(listOf("$integrationTestDirectory/resources"))
          val pluginClasspath = configurations.getByName(JENKINS_PLUGIN_CLASSPATH_CONFIGURATION)
          compileClasspath += pluginClasspath
          runtimeClasspath += pluginClasspath
        }

      configurations {
        val testHarness =
          create(TEST_HARNESS_CONFIGURATION) {
            isCanBeResolved = true
            isCanBeConsumed = false
            isVisible = false
            description = "jenkins-test-harness and its transitive dependencies"
            withDependencies {
              LOGGER.debug { "Adding jenkins-test-harness to configuration $name" }
              dependencyHandler.add(name, sharedLibraryExtension.testHarnessDependency().get())
            }
          }
        getByName(integrationTestSourceSet.implementationConfigurationName) {
          extendsFrom(testHarness)
        }
        getByName(integrationTestSourceSet.runtimeOnlyConfigurationName) {
          extendsFrom(configurations.getByName(JENKINS_PLUGIN_HPIS_CONFIGURATION))
        }
      }

      tasks {
        val integrationTest =
          register<Test>(INTEGRATION_TEST_TASK) {
            dependsOn(sourceSets.main.classesTaskName)
            mustRunAfter("test")
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Runs tests with the jenkins-test-harness"
            testClassesDirs = integrationTestSourceSet.output.classesDirs
            classpath = integrationTestSourceSet.runtimeClasspath
            systemProperty(
              "buildDirectory",
              projectLayout.buildDirectory.map { it.asFile.absolutePath },
            )
            shouldRunAfter("test")
          }
        named(LifecycleBasePlugin.CHECK_TASK_NAME) {
          dependsOn(integrationTest)
        }
      }
    }

    private fun Project.setupIvyGrabSupport() {
      val ivy =
        configurations.create(IVY_CONFIGURATION) {
          isCanBeResolved = true
          isCanBeConsumed = false
          isVisible = false
          description = "Ivy for @Grab support in shared library Groovy sources"
        }
      dependencies.add(ivy.name, IVY_COORDINATES)
      tasks {
        withType<GroovyCompile>().configureEach {
          groovyClasspath += ivy
        }
        "test"(Test::class) {
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

    private fun Project.setupSharedLibraryExtension() {
      val pipelineTestUnitVersion = initializedProperty(DEFAULT_JENKINS_PIPELINE_UNIT_VERSION)
      val testHarnessVersion = initializedProperty(DEFAULT_TEST_HARNESS_VERSION)
      extensions.create(
        SHARED_LIBRARY_EXTENSION_NAME,
        SharedLibraryExtension::class,
        pipelineTestUnitVersion,
        testHarnessVersion,
      )
    }

    private val Project.sharedLibraryExtension: SharedLibraryExtension
      get() = the()

    private val Project.sourceSets: SourceSetContainer
      get() = the()

    private val SourceSetContainer.main: SourceSet
      get() = getByName("main")

    private val SourceSetContainer.test: SourceSet
      get() = getByName("test")

    private val SourceSet.groovy: SourceDirectorySet
      get() = extensions.getByType(GroovySourceDirectorySet::class.java)

    private fun Configuration.defaultJenkinsConfigurationSetup(canBeResolved: Boolean = false) {
      isCanBeResolved = canBeResolved
      isVisible = false
      isCanBeConsumed = false
    }
  }
