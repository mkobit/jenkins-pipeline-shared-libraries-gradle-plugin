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
import org.gradle.testing.base.TestingExtension
import javax.inject.Inject

open class SharedLibraryPlugin
  @Inject
  constructor(
    private val projectLayout: ProjectLayout,
  ) : Plugin<Project> {
    companion object {
      private const val DEFAULT_JENKINS_PIPELINE_UNIT_VERSION = "1.29"
      private const val DEFAULT_TEST_HARNESS_VERSION = "2.50"
      private const val DEFAULT_CORE_VERSION = "2.479.1"

      private const val INTEGRATION_TEST_SUITE = "integrationTest"

      private const val JENKINS_PLUGIN_CONFIGURATION = "jenkinsPlugin"
      private const val JENKINS_PLUGIN_CLASSPATH_CONFIGURATION = "jenkinsPluginClasspath"
      private const val JENKINS_PLUGIN_HPIS_CONFIGURATION = "jenkinsPluginHpis"

      private const val IVY_CONFIGURATION = "sharedLibraryIvy"
      private const val IVY_COORDINATES = "org.apache.ivy:ivy:2.4.0"
    }

    override fun apply(target: Project) {
      target.run {
        apply<GroovyPlugin>()
        apply<JenkinsIntegrationPlugin>()
        setupJenkinsPluginConfiguration()
        setupMain()
        setupTestSuites()
        setupDocumentationTasks()
        setupIvyGrabSupport()
      }
    }

    @Suppress("DEPRECATION")
    private fun Project.setupJenkinsPluginConfiguration() {
      dependencies.components.all(JenkinsPluginRule::class.java)
      dependencies.attributesSchema.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE) {
        compatibilityRules.add(JpiCompatibilityRule::class.java)
      }

      val dependencyHandler = dependencies

      // Eagerly created so the Kotlin DSL generates the jenkinsPlugin(...) typed accessor at sync time.
      val jenkinsPlugin =
        configurations.create(JENKINS_PLUGIN_CONFIGURATION) {
          isCanBeResolved = false
          isCanBeConsumed = false
          description = "Jenkins HPI/JPI plugin dependencies for shared library compilation and testing"
        }

      // Internal: JARs for compilation and unit tests.
      // jenkins-core is not in any published HPI's compile variant so it must be added explicitly.
      configurations.create(JENKINS_PLUGIN_CLASSPATH_CONFIGURATION) {
        isCanBeResolved = true
        isCanBeConsumed = false
        isVisible = false
        description = "Jenkins plugin JARs + jenkins-core for compilation and unit tests"
        extendsFrom(jenkinsPlugin)
        withDependencies {
          dependencyHandler.add(name, "org.jenkins-ci.main:jenkins-core:$DEFAULT_CORE_VERSION")
        }
        attributes {
          attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE)
        }
      }

      // Internal: raw HPI archives for the embedded Jenkins runtime used by JenkinsRule.
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

    private fun Project.setupTestSuites() {
      val pluginClasspath = configurations.getByName(JENKINS_PLUGIN_CLASSPATH_CONFIGURATION)

      // Lenient view so plain-JAR transitives that don't publish HPI are silently skipped
      // rather than failing resolution when artifactType=hpi is requested globally.
      val hpiFiles =
        configurations
          .getByName(JENKINS_PLUGIN_HPIS_CONFIGURATION)
          .incoming
          .artifactView { isLenient = true }
          .artifacts
          .artifactFiles

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

      extensions.configure<TestingExtension> {
        suites {
          withType<JvmTestSuite>().configureEach {
            sources.compileClasspath += pluginClasspath
            sources.runtimeClasspath += pluginClasspath
          }

          val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
            sources.apply {
              java.setSrcDirs(emptyList<Any>())
              groovy.setSrcDirs(listOf("test/unit/groovy"))
              resources.setSrcDirs(listOf("test/unit/resources"))
            }
            dependencies {
              implementation("com.lesfurets:jenkins-pipeline-unit:$DEFAULT_JENKINS_PIPELINE_UNIT_VERSION")
            }
          }

          // JPU 1.29 transitively brings groovy-all:2.4 which conflicts with Spock's Groovy 3 AST transform.
          // Exclude groovy-all so the test runtime uses a single Groovy version (3.x from Spock).
          configurations.named("testImplementation") {
            exclude(group = "org.codehaus.groovy", module = "groovy-all")
          }

          val integrationTest by registering(JvmTestSuite::class) {
            sources.apply {
              java.setSrcDirs(emptyList<Any>())
              groovy.setSrcDirs(listOf("test/integration/groovy"))
              resources.setSrcDirs(listOf("test/integration/resources"))
            }
            dependencies {
              implementation("org.jenkins-ci.main:jenkins-test-harness:$DEFAULT_TEST_HARNESS_VERSION")
            }
            targets.all {
              testTask.configure {
                mustRunAfter(test)
                description = "Runs integration tests against an embedded Jenkins runtime"
                classpath += hpiFiles
                systemProperty("test.library.src", srcDir)
                systemProperty("test.library.vars", varsDir)
                systemProperty("test.library.resources", resourcesDir)
              }
            }
          }
        }
      }

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
          isVisible = false
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
