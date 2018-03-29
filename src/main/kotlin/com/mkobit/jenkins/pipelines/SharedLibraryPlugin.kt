package com.mkobit.jenkins.pipelines

import com.mkobit.jenkins.pipelines.codegen.GenerateJavaFile
import mu.KotlinLogging
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.GroovySourceSet
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.javadoc.Groovydoc
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
// @formatter:off
import org.gradle.kotlin.dsl.* // * import easiest to handle https://github.com/gradle/kotlin-dsl/issues/564
// @formatter:on
import org.gradle.language.base.plugins.LifecycleBasePlugin
import javax.inject.Inject

open class SharedLibraryPlugin @Inject constructor(
  private val projectLayout: ProjectLayout,
  private val objectFactory: ObjectFactory
) : Plugin<Project> {

  companion object {
    private val LOGGER = KotlinLogging.logger {}
    /**
     * Name of the [org.gradle.api.artifacts.repositories.ArtifactRepository] that is added to the [RepositoryHandler].
     */
    const val JENKINS_REPOSITORY_NAME = "JenkinsPublic"
    /**
     * URL of the [org.gradle.api.artifacts.repositories.ArtifactRepository] that is added to the [RepositoryHandler].
     * @see JENKINS_REPOSITORY_NAME
     */
    const val JENKINS_REPOSITORY_URL = "https://repo.jenkins-ci.org/public/"
    private const val SHARED_LIBRARY_EXTENSION_NAME = "sharedLibrary"
    private const val TEST_ROOT_PATH = "test"
    private const val DEFAULT_JENKINS_PIPELINE_UNIT_VERSION = "1.1"
    private const val DEFAULT_CORE_VERSION = "2.89.4"
    private const val DEFAULT_TEST_HARNESS_VERSION = "2.34"
    private const val DEFAULT_WORKFLOW_API_PLUGIN_VERSION = "2.26"
    private const val DEFAULT_WORKFLOW_BASIC_STEPS_PLUGIN_VERSION = "2.6"
    private const val DEFAULT_WORKFLOW_CPS_PLUGIN_VERSION = "2.45"
    private const val DEFAULT_WORKFLOW_DURABLE_TASK_STEP_PLUGIN_VERSION = "2.19"
    private const val DEFAULT_WORKFLOW_GLOBAL_CPS_LIBRARY_PLUGIN_VERSION = "2.9"
    private const val DEFAULT_WORKFLOW_JOB_PLUGIN_VERSION = "2.17"
    private const val DEFAULT_WORKFLOW_MULTIBRANCH_PLUGIN_VERSION = "2.17"
    private const val DEFAULT_WORKFLOW_STEP_API_PLUGIN_VERSION = "2.14"
    private const val DEFAULT_WORKFLOW_SCM_STEP_PLUGIN_VERSION = "2.6"
    private const val DEFAULT_WORKFLOW_SUPPORT_PLUGIN_VERSION = "2.18"

    private const val INTEGRATION_TEST_SOURCE_SET = "integrationTest"
    private const val INTEGRATION_TEST_TASK = "integrationTest"

    // This configuration is the main configuration that contains the plugin dependencies.
    private const val JENKINS_PLUGINS_CONFIGURATION = "jenkinsPlugins"

    private const val GROOVY_CONFIGURATION = "sharedLibraryGroovy"
    // These are internal configurations used in the compilation and runtime
    private const val UNIT_TESTING_LIBRARY_CONFIGURATION = "jenkinsPipelineUnitTestLibraries"
    private const val PLUGIN_HPI_JPI_CONFIGURATION = "jenkinsPluginHpisAndJpis"
    private const val PLUGIN_LIBRARY_CONFIGURATION = "jenkinsPluginLibraries"
    private const val CORE_LIBRARY_CONFIGURATION = "jenkinsCoreLibraries"
    private const val TEST_LIBRARY_CONFIGURATION = "jenkinsTestLibraries"
    private const val TEST_LIBRARY_RUNTIME_ONLY_CONFIGURATION = "jenkinsTestLibrariesRuntimeOnly"

    private const val IVY_CONFIGURATION = "sharedLibraryIvy"
    private const val IVY_COORDINATES = "org.apache.ivy:ivy:2.4.0"
  }

  override fun apply(target: Project) {
    target.run {
      // Ordering of calls here matters
      pluginManager.apply(GroovyPlugin::class.java)
      pluginManager.apply(JenkinsIntegrationPlugin::class.java)
      setupJenkinsRepository()
      setupJavaDefaults()
      setupSharedLibraryExtension()
      setupConfigurationsAndDependencies()
      setupMain()
      setupUnitTest()
      setupIntegrationTest()
      setupDocumentationTasks()
      setupIvyGrabSupport()
    }
  }

  private fun Project.setupJavaDefaults() {
    java.sourceCompatibility = JavaVersion.VERSION_1_8
    java.targetCompatibility = JavaVersion.VERSION_1_8
  }

  /**
   * Sets up the `main` source set.
   */
  private fun Project.setupMain() {
    val main = java.sourceSets.main.apply {
      java.setSrcDirs(emptyList<String>())
      groovy.setSrcDirs(listOf("src", "vars"))
      resources.setSrcDirs(listOf("resources"))
    }

    configurations {
      main.implementationConfigurationName().extendsFrom(
        sharedLibraryGroovy,
        jenkinsCoreLibraries,
        jenkinsPluginLibraries)
    }
  }

  /**
   * Sets up the `test` source set.
   */
  private fun Project.setupUnitTest() {
    val dependencyHandler = dependencies
    val test = java.sourceSets.test.apply {
      val unitTestDirectory = "$TEST_ROOT_PATH/unit"
      java.setSrcDirs(listOf("$unitTestDirectory/java"))
      groovy.setSrcDirs(listOf("$unitTestDirectory/groovy"))
      resources.setSrcDirs(listOf("$unitTestDirectory/resources"))
    }
    configurations {
      val configuration = UNIT_TESTING_LIBRARY_CONFIGURATION {
        description = "Jenkins Pipeline Unit libraries"
        defaultJenkinsConfigurationSetup()
        withDependencies {
          LOGGER.debug { "Adding JenkinsPipelineUnit dependency to configuration${this@UNIT_TESTING_LIBRARY_CONFIGURATION.name}" }
          dependencyHandler.add(this@UNIT_TESTING_LIBRARY_CONFIGURATION, extensions.sharedLibraryExtension.pipelineUnitDependency().get())
        }
      }
      test.implementationConfigurationName().extendsFrom(configuration, sharedLibraryGroovy)
    }
  }

  /**
   * Sets up all configurations and dependencies.
   */
  private fun Project.setupConfigurationsAndDependencies() {
    setupJenkinsPluginsDependencies()
    setupPluginHpiAndJpiDependencies()
    setupPluginLibraryDependencies()
    setupCoreLibraryDependencies()
    setupTestLibraryDependencies()
    setupTestLibraryRuntimeDependencies()
    setupGroovyConfiguration()
  }

  /**
   * Creates and sets up the [JENKINS_PLUGINS_CONFIGURATION] configuration to map dependencies from the
   * [SharedLibraryExtension].
   */
  private fun Project.setupJenkinsPluginsDependencies() {
    val dependencyHandler = dependencies
    configurations {
      JENKINS_PLUGINS_CONFIGURATION {
        defaultJenkinsConfigurationSetup(canBeResolved = true)
        description = "Jenkins plugin dependencies"
        withDependencies {
          LOGGER.debug { "Adding plugin dependencies from ${SharedLibraryExtension::class.java.canonicalName} to configuration ${this@JENKINS_PLUGINS_CONFIGURATION.name}" }
          // TODO: remove pluginDependencies().pluginDependencies() confusing method calls
          extensions.sharedLibraryExtension.pluginDependencies()
            .pluginDependencies()
            .get()
            .forEach { dependencyHandler.add(this@JENKINS_PLUGINS_CONFIGURATION, it.asStringNotation()) }
        }
      }
    }
  }

  private fun Project.setupPluginHpiAndJpiDependencies() {
    val dependencyHandler = dependencies
    configurations {
      PLUGIN_HPI_JPI_CONFIGURATION {
        defaultJenkinsConfigurationSetup()
        description = "Jenkins plugin HPI and JPI dependencies needed at runtime"
        withDependencies {
          jenkinsPlugins.resolvedConfiguration.resolvedArtifacts
            .filter { it.extension in setOf("hpi", "jpi") }
            .map { "${it.moduleVersion}@${it.extension}" }
            .forEach { dependencyHandler.add(this@PLUGIN_HPI_JPI_CONFIGURATION, it) }
        }
      }
    }
  }

  private fun Project.setupPluginLibraryDependencies() {
    val dependencyHandler = dependencies
    configurations {
      PLUGIN_LIBRARY_CONFIGURATION {
        defaultJenkinsConfigurationSetup()
        description = "Jenkins plugin JAR libraries"
        withDependencies {
          // Map each included HPI to that plugin's JAR for usage in compilation of tests and also
          // include all of the additional JAR dependencies from the transitive dependencies of the plugin
          jenkinsPlugins.resolvedConfiguration.resolvedArtifacts
            .filter { it.extension in setOf("hpi", "jpi", "jar") }
            .map { "${it.moduleVersion}@jar" } // Use the published JAR libraries for each plugin
            .forEach { dependencyHandler.add(this@PLUGIN_LIBRARY_CONFIGURATION, it) }

          // TODO: hack - total hack to get the stupid servlet API dependency on the classpath
          // since I can't seem to pull it from the POM of Jenkins Core.
          // see https://discuss.gradle.org/t/how-to-query-for-provided-scoped-dependencies-of-a-configuration/26131
          // This probably won't work for older versions of Jenkins because of different servlet-api
          // versions, but should work enough for now to enable usage of Jenkins core and plugin classes in the library
          // source
          dependencyHandler.add(this@PLUGIN_LIBRARY_CONFIGURATION, "javax.servlet:javax.servlet-api:3.1.0")
        }
      }
    }
  }

  private fun Project.setupCoreLibraryDependencies() {
    val dependencyHandler = dependencies
    configurations {
      CORE_LIBRARY_CONFIGURATION {
        defaultJenkinsConfigurationSetup(canBeResolved = true)
        description = "Jenkins core libraries and modules"
        withDependencies {
          dependencyHandler.add(this@CORE_LIBRARY_CONFIGURATION, extensions.sharedLibraryExtension.coreDependency().get())
        }
      }
    }
  }

  private fun Project.setupTestLibraryDependencies() {
    val dependencyHandler = dependencies
    configurations {
      TEST_LIBRARY_CONFIGURATION {
        defaultJenkinsConfigurationSetup()
        description = "Jenkins test harness and modules"
        withDependencies {
          dependencyHandler.add(this@TEST_LIBRARY_CONFIGURATION, extensions.sharedLibraryExtension.testHarnessDependency().get())
        }
      }
    }
  }

  private fun Project.setupTestLibraryRuntimeDependencies() {
    val dependencyHandler = dependencies
    configurations {
      TEST_LIBRARY_RUNTIME_ONLY_CONFIGURATION {
        defaultJenkinsConfigurationSetup()
        description = "Jenkins test runtime libraries"
        withDependencies {
          dependencyHandler.add(this@TEST_LIBRARY_RUNTIME_ONLY_CONFIGURATION, "${extensions.sharedLibraryExtension.jenkinsWar().get()}@war")
        }
      }
    }
  }

  /**
   * Sets up support for using `@Grab` in `main` source and `test`
   */
  private fun Project.setupIvyGrabSupport() {
    val ivy = configurations.create(IVY_CONFIGURATION) {
      defaultJenkinsConfigurationSetup(canBeResolved = true)
      description = "Ivy configuration for @Grab support"
    }
    dependencies.add(ivy, IVY_COORDINATES)
    tasks {
      withType<GroovyCompile> {
        groovyClasspath += ivy
      }
      "test"(Test::class) {
        classpath += ivy
      }
    }
  }

  /**
   * Sets up Groovydoc JAR and source JAR tasks.
   */
  private fun Project.setupDocumentationTasks() {
    tasks {
      "sourcesJar"(Jar::class) {
        description = "Assemble the sources JAR"
        classifier = "sources"
        from(java.sourceSets.main.allSource)
      }
      "groovydocJar"(Jar::class) {
        val groovydoc = GroovyPlugin.GROOVYDOC_TASK_NAME(Groovydoc::class)
        dependsOn(groovydoc)
        description = "Assemble the Groovydoc JAR"
        classifier = "javadoc"
      }
    }
  }

  /**
   * Creates and sets up the `integrationTest` source set.
   */
  private fun Project.setupIntegrationTest() {
    val generatedIntegrationSources = projectLayout.buildDirectory.dir("generated-src/integrationTest")
    java.sourceSets {
      INTEGRATION_TEST_SOURCE_SET {
        description = "Integration test set for shared library source"
        val integrationTestDirectory = "$TEST_ROOT_PATH/integration"
        java.setSrcDirs(listOf("$integrationTestDirectory/java"))
        groovy.setSrcDirs(listOf("$integrationTestDirectory/groovy", generatedIntegrationSources))
        resources.setSrcDirs(listOf("$integrationTestDirectory/resources"))
        val generateLocalLibraryRetriever by tasks.creating(GenerateJavaFile::class) {
          description = "Generates a LibraryRetriever implementation for easier writing of integration tests"
          srcDir.set(generatedIntegrationSources)
          javaFile.set(localLibraryAdder())
        }
        tasks[getCompileTaskName("groovy")].dependsOn(generateLocalLibraryRetriever)
      }
    }

    configurations {
      java.sourceSets.integrationTest.implementationConfigurationName {
        extendsFrom(
          sharedLibraryGroovy,
          jenkinsCoreLibraries,
          jenkinsPluginLibraries,
          jenkinsTestLibraries
        )
      }

      java.sourceSets.integrationTest.runtimeOnlyConfigurationName().extendsFrom(
        jenkinsPluginHpisAndJpis,
        jenkinsTestLibrariesRuntimeOnly
      )
    }

    tasks {
      val integrationTest = INTEGRATION_TEST_TASK(Test::class) {
        dependsOn(java.sourceSets.main.classesTaskName)
        mustRunAfter("test")
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Runs tests with the jenkins-test-harness"
        testClassesDirs = java.sourceSets.integrationTest.output.classesDirs
        classpath = java.sourceSets.integrationTest.runtimeClasspath
        // Set the build directory for Jenkins test harness.
        // See https://issues.jenkins-ci.org/browse/JENKINS-26331
        systemProperty("buildDirectory", projectLayout.buildDirectory.get().asFile.absolutePath)
        shouldRunAfter("test")
      }
      LifecycleBasePlugin.CHECK_TASK_NAME().dependsOn(integrationTest)
    }
  }

  /**
   * Creates and sets up a configuration for the Groovy dependency.
   */
  private fun Project.setupGroovyConfiguration() {
    val dependencyHandler = dependencies
    configurations {
      GROOVY_CONFIGURATION {
        defaultJenkinsConfigurationSetup()
        description = "Shared Library Groovy dependency"
        withDependencies {
          val groovyArtifact = jenkinsCoreLibraries.resolvedConfiguration.resolvedArtifacts.find {
            it.moduleVersion.id.group == "org.codehaus.groovy"
          } ?: throw GradleException("Could not find Groovy dependency as Jenkins core Library")
          LOGGER.debug { "Adding ${groovyArtifact.moduleVersion} to ${this@GROOVY_CONFIGURATION.name}" }
          dependencyHandler.add(this@GROOVY_CONFIGURATION, groovyArtifact.moduleVersion.id.toString())
        }
      }
    }
  }

  /**
   * Creates and sets up a repository for [JENKINS_REPOSITORY_URL].
   */
  private fun Project.setupJenkinsRepository() {
    LOGGER.debug { "Adding repository named $JENKINS_REPOSITORY_NAME with URL $JENKINS_REPOSITORY_URL" }
    repositories.maven {
      name = JENKINS_REPOSITORY_NAME
      setUrl(JENKINS_REPOSITORY_URL)
      metadataSources {
        mavenPom()
      }
    }
  }

  /**
   * Creates the [SharedLibraryExtension] with the default versions.
   */
  private fun Project.setupSharedLibraryExtension() {
    val coreVersion = initializedProperty(DEFAULT_CORE_VERSION)
    val pipelineTestUnitVersion = initializedProperty(DEFAULT_JENKINS_PIPELINE_UNIT_VERSION)
    val testHarnessVersion = initializedProperty(DEFAULT_TEST_HARNESS_VERSION)
    // TODO: find a better DSL for managing these dependencies, possibly by using aggregator plugin because we are
    // most likely missing some
    val workflowApiPluginVersion = initializedProperty(DEFAULT_WORKFLOW_API_PLUGIN_VERSION)
    val workflowBasicStepsPluginVersion = initializedProperty(
      DEFAULT_WORKFLOW_BASIC_STEPS_PLUGIN_VERSION)
    val workflowCpsPluginVersion = initializedProperty(DEFAULT_WORKFLOW_CPS_PLUGIN_VERSION)
    val workflowDurableTaskStepPluginVersion = initializedProperty(
      DEFAULT_WORKFLOW_DURABLE_TASK_STEP_PLUGIN_VERSION)
    val workflowGlobalCpsLibraryPluginVersion = initializedProperty(
      DEFAULT_WORKFLOW_GLOBAL_CPS_LIBRARY_PLUGIN_VERSION)
    val workflowJobPluginVersion = initializedProperty(DEFAULT_WORKFLOW_JOB_PLUGIN_VERSION)
    val workflowMultibranchPluginVersion = initializedProperty(DEFAULT_WORKFLOW_MULTIBRANCH_PLUGIN_VERSION)
    val workflowStepApiPluginVersion = initializedProperty(DEFAULT_WORKFLOW_STEP_API_PLUGIN_VERSION)
    val workflowScmStepPluginVersion = initializedProperty(DEFAULT_WORKFLOW_SCM_STEP_PLUGIN_VERSION)
    val workflowSupportPluginVersion = initializedProperty(DEFAULT_WORKFLOW_SUPPORT_PLUGIN_VERSION)

    val pluginDependencySpec = objectFactory.newInstance(PluginDependencySpec::class.java,
      workflowApiPluginVersion,
      workflowBasicStepsPluginVersion,
      workflowCpsPluginVersion,
      workflowDurableTaskStepPluginVersion,
      workflowGlobalCpsLibraryPluginVersion,
      workflowJobPluginVersion,
      workflowMultibranchPluginVersion,
      workflowScmStepPluginVersion,
      workflowStepApiPluginVersion,
      workflowSupportPluginVersion,
      objectFactory
    )
    extensions.create(
      SHARED_LIBRARY_EXTENSION_NAME,
      SharedLibraryExtension::class.java,
      coreVersion,
      pipelineTestUnitVersion,
      testHarnessVersion,
      pluginDependencySpec
    )
  }

  /**
   * Default setup for configurations related to this plugin.
   */
  private fun Configuration.defaultJenkinsConfigurationSetup(canBeResolved: Boolean = false) {
    isCanBeResolved = canBeResolved
    isVisible = false
    isCanBeConsumed = false
  }

  /**
   * Gets the configuration with name [JENKINS_PLUGINS_CONFIGURATION].
   */
  private val NamedDomainObjectContainerScope<Configuration>.jenkinsPlugins
    get() = maybeCreate(JENKINS_PLUGINS_CONFIGURATION)

  /**
   * Gets the configuration with name [CORE_LIBRARY_CONFIGURATION].
   */
  private val NamedDomainObjectContainerScope<Configuration>.jenkinsCoreLibraries
    get() = maybeCreate(CORE_LIBRARY_CONFIGURATION)

  /**
   * Gets the configuration with name [PLUGIN_LIBRARY_CONFIGURATION].
   */
  private val NamedDomainObjectContainerScope<Configuration>.jenkinsPluginLibraries
    get() = maybeCreate(PLUGIN_LIBRARY_CONFIGURATION)

  /**
   * Gets the configuration with name [PLUGIN_HPI_JPI_CONFIGURATION].
   */
  private val NamedDomainObjectContainerScope<Configuration>.jenkinsPluginHpisAndJpis
    get() = maybeCreate(PLUGIN_HPI_JPI_CONFIGURATION)

  /**
   * Gets the configuration with name [TEST_LIBRARY_CONFIGURATION].
   */
  private val NamedDomainObjectContainerScope<Configuration>.jenkinsTestLibraries
    get() = maybeCreate(TEST_LIBRARY_CONFIGURATION)

  /**
   * Gets the configuration with name [TEST_LIBRARY_RUNTIME_ONLY_CONFIGURATION].
   */
  private val NamedDomainObjectContainerScope<Configuration>.jenkinsTestLibrariesRuntimeOnly
    get() = maybeCreate(TEST_LIBRARY_RUNTIME_ONLY_CONFIGURATION)

  /**
   * Gets the configuration with name [GROOVY_CONFIGURATION].
   */
  private val NamedDomainObjectContainerScope<Configuration>.sharedLibraryGroovy
    get() = getByName(GROOVY_CONFIGURATION)

  /**
   * Gets the extension of type [SharedLibraryExtension].
   */
  private val ExtensionContainer.sharedLibraryExtension: SharedLibraryExtension
    get() = getByType(SharedLibraryExtension::class.java)

  /**
   * Gets the [JavaPluginConvention].
   */
  private val Project.java: JavaPluginConvention
    get() = withConvention(JavaPluginConvention::class) { this }

  /**
   * Gets the source set with name "main".
   */
  private val SourceSetContainer.main: SourceSet
    get() = getByName("main")

  /**
   * Gets the source set with name "test".
   */
  private val SourceSetContainer.test: SourceSet
    get() = getByName("test")

  /**
   * Gets the source set with name "integrationTest".
   */
  private val SourceSetContainer.integrationTest: SourceSet
    get() = getByName("integrationTest")

  /**
   * Gets the Groovy source for source set.
   */
  private val SourceSet.groovy: SourceDirectorySet
    get() = withConvention(GroovySourceSet::class) { groovy }

  /**
   * Adds a dependency to the given configuration.
   * @param configuration the configuration
   * @param dependencyNotation the dependency notation
   */
  private fun DependencyHandler.add(configuration: Configuration, dependencyNotation: Any): Dependency =
    add(configuration.name, dependencyNotation)
}
