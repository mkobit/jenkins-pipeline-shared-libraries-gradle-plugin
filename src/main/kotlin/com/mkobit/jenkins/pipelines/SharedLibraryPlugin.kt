package com.mkobit.jenkins.pipelines

import com.mkobit.jenkins.pipelines.codegen.GenerateJavaFile
import mu.KotlinLogging
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.file.ProjectLayout
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.GroovySourceSet
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.javadoc.Groovydoc
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getValue // this is actually used, see https://github.com/gradle/kotlin-dsl/issues/564
import org.gradle.kotlin.dsl.getting
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.withConvention
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin
import javax.inject.Inject

open class SharedLibraryPlugin @Inject constructor(
  private val projectLayout: ProjectLayout
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
    private const val DEFAULT_GROOVY_VERSION = "2.4.11"
    private const val DEFAULT_CORE_VERSION = "2.89.2"
    private const val DEFAULT_TEST_HARNESS_VERSION = "2.33"
    private const val DEFAULT_WORKFLOW_API_PLUGIN_VERSION = "2.24"
    private const val DEFAULT_WORKFLOW_BASIC_STEPS_PLUGIN_VERSION = "2.6"
    private const val DEFAULT_WORKFLOW_CPS_PLUGIN_VERSION = "2.42"
    private const val DEFAULT_WORKFLOW_DURABLE_TASK_STEP_PLUGIN_VERSION = "2.17"
    private const val DEFAULT_WORKFLOW_GLOBAL_CPS_LIBRARY_PLUGIN_VERSION = "2.9"
    private const val DEFAULT_WORKFLOW_JOB_PLUGIN_VERSION = "2.16"
    private const val DEFAULT_WORKFLOW_MULTIBRANCH_PLUGIN_VERSION = "2.16"
    private const val DEFAULT_WORKFLOW_STEP_API_PLUGIN_VERSION = "2.14"
    private const val DEFAULT_WORKFLOW_SCM_STEP_PLUGIN_VERSION = "2.6"
    private const val DEFAULT_WORKFLOW_SUPPORT_PLUGIN_VERSION = "2.16"

    // This configuration is used for an initial resolution to get the required dependencies
    private const val JENKINS_PLUGINS_CONFIGURATION = "jenkinsPlugins"

    private const val GROOVY_CONFIGURATION = "sharedLibraryGroovy"
    // These are internal configurations used in the compilation and runtime
    private const val UNIT_TESTING_LIBRARY_CONFIGURATION = "jenkinsPipelineUnitTestLibraries"
    // These are both unused because we use the kotlin-dsl to create and set them up right now.
    @Suppress("UNUSED")
    private const val PLUGIN_HPI_JPI_CONFIGURATION = "jenkinsPluginHpisAndJpis"
    @Suppress("UNUSED")
    private const val PLUGIN_LIBRARY_CONFIGURATION = "jenkinsPluginLibraries"
    private const val CORE_LIBRARY_CONFIGURATION = "jenkinsCoreLibraries"
    private const val TEST_LIBRARY_CONFIGURATION = "jenkinsTestLibraries"
    private const val JENKINS_LIBRARIES_COMPILE_ONLY_CONFIGURATION = "jenkinsLibrariesMainCompileOnly"
    private const val TEST_LIBRARY_RUNTIME_ONLY_CONFIGURATION = "jenkinsTestLibrariesRuntimeOnly"

    private const val IVY_CONFIGURATION = "globalLibraryIvy"
    private const val IVY_COORDINATES = "org.apache.ivy:ivy:2.4.0"
  }

  override fun apply(target: Project) {
    target.run {
      pluginManager.apply(GroovyPlugin::class.java)
      pluginManager.apply(JenkinsIntegrationPlugin::class.java)
      setupJenkinsRepository()
      withConvention(JavaPluginConvention::class) { setupJava(this, tasks) }
      createSharedLibraryExtension()
      setupJenkinsPluginsDependencies()
      setupGroovyConfiguration()
      setupMain()
      setupUnitTest()
      setupIntegrationTestTask()
      setupDocumentationTasks()
      setupConfigurationsAndDependencyManagement()
      setupIvyGrabSupport()
      addDependenciesFromExtension()
    }
  }

  private fun Project.setupMain() {
    val dependencyHandler = dependencies
    configurations {
      val jenkinsLibrariesMainCompileOnly= JENKINS_LIBRARIES_COMPILE_ONLY_CONFIGURATION {
        withDependencies {
          // For @NonCPS in source code, need to add a CloudBees Groovy CPS dependency
          jenkinsPlugins.resolvedConfiguration.resolvedArtifacts
            .filter { it.moduleVersion.id.group == "com.cloudbees" && it.moduleVersion.id.name == "groovy-cps" }
            .forEach { dependencyHandler.add(this@JENKINS_LIBRARIES_COMPILE_ONLY_CONFIGURATION, it.moduleVersion.toString()) }
        }
      }
      java.sourceSets.main.compileOnlyConfigurationName().extendsFrom(jenkinsLibrariesMainCompileOnly)
      java.sourceSets.main.implementationConfigurationName().extendsFrom(sharedLibraryGroovy)
    }
  }

  private fun Project.setupUnitTest() {
    val dependencyHandler = dependencies
    configurations {
      val configuration = UNIT_TESTING_LIBRARY_CONFIGURATION {
        defaultJenkinsConfigurationSetup()
        withDependencies {
          LOGGER.debug { "Adding JenkinsPipelineUnit dependency to configuration${this@UNIT_TESTING_LIBRARY_CONFIGURATION.name}" }
          dependencyHandler.add(this@UNIT_TESTING_LIBRARY_CONFIGURATION, extensions.sharedLibraryExtension.pipelineUnitDependency())
        }
      }
      java.sourceSets.test.implementationConfigurationName().extendsFrom(configuration, sharedLibraryGroovy)
    }
  }

  private fun Project.setupJenkinsPluginsDependencies() {
    val dependencyHandler = dependencies
    configurations {
      JENKINS_PLUGINS_CONFIGURATION {
        isCanBeResolved = true
        isVisible = false
        isCanBeConsumed = false
        withDependencies {
          LOGGER.debug { "Adding plugin dependencies from ${SharedLibraryExtension::class.java.canonicalName} to configuration ${this@JENKINS_PLUGINS_CONFIGURATION.name}" }
          // TODO: remove pluginDependencies().pluginDependencies() confusing method calls
          extensions.sharedLibraryExtension.pluginDependencies()
            .pluginDependencies()
            .map { dependencyHandler.createExternal(it.asStringNotation()) }
            .forEach { dependencyHandler.add(this@JENKINS_PLUGINS_CONFIGURATION, it) }
        }
      }
    }
  }

  private fun Project.setupIvyGrabSupport() {
    val ivy = configurations.create(IVY_CONFIGURATION) {
      defaultJenkinsConfigurationSetup()
    }
    dependencies.add(ivy, IVY_COORDINATES)
    tasks.withType<GroovyCompile> {
      groovyClasspath += ivy
    }
    tasks {
      "test"(Test::class) {
        classpath += ivy
      }
    }
  }

  private fun Project.addDependenciesFromExtension() {
    val dependencyHandler = dependencies
    configurations {
      TEST_LIBRARY_CONFIGURATION {
        withDependencies {
          dependencyHandler.add(this@TEST_LIBRARY_CONFIGURATION, extensions.sharedLibraryExtension.testHarnessDependency())
        }
      }
      TEST_LIBRARY_RUNTIME_ONLY_CONFIGURATION {
        withDependencies {
          dependencyHandler.add(this@TEST_LIBRARY_RUNTIME_ONLY_CONFIGURATION, "${extensions.sharedLibraryExtension.jenkinsWar()}@war")
        }
      }
      CORE_LIBRARY_CONFIGURATION {
        withDependencies {
          dependencyHandler.add(this@CORE_LIBRARY_CONFIGURATION, extensions.sharedLibraryExtension.coreDependency())
        }
      }
    }
  }

  private fun Project.setupDocumentationTasks() {
    tasks {
      "sourcesJar"(Jar::class) {
        description = "Assemble the sources JAR"
        classifier = "sources"
        from(java.sourceSets.main.allSource)
      }
      "groovydocJar"(Jar::class) {
        val groovydoc = tasks.getByName(GroovyPlugin.GROOVYDOC_TASK_NAME) as Groovydoc
        dependsOn(groovydoc)
        description = "Assemble the Groovydoc JAR"
        classifier = "javadoc"
      }
    }
  }

  private fun Project.setupIntegrationTestTask() {
    val integrationTest by tasks.creating(Test::class.java) {
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
    tasks[LifecycleBasePlugin.CHECK_TASK_NAME].dependsOn(integrationTest)
  }

  private fun Project.setupConfigurationsAndDependencyManagement() {
    val dependencyHandler = dependencies
    val configurationAction: Configuration.() -> Unit = {
      isCanBeResolved = true
      isVisible = false
      isCanBeConsumed = false
    }

    val jenkinsPluginHpisAndJpis by configurations.creating(configurationAction)
    val jenkinsPluginLibraries by configurations.creating(configurationAction)
    val jenkinsCoreLibraries by configurations.creating(configurationAction)
    val jenkinsTestLibraries by configurations.creating(configurationAction)
    val jenkinsTestLibrariesRuntimeOnly by configurations.creating(configurationAction)

    configurations {
      java.sourceSets.integrationTest.implementationConfigurationName() {
        extendsFrom(
          sharedLibraryGroovy,
          getByName(java.sourceSets.test.implementationConfigurationName),
          jenkinsCoreLibraries,
          jenkinsPluginLibraries,
          jenkinsTestLibraries
        )
        exclude(group = "com.lesfurets", module = "jenkins-pipeline-unit")
      }

      java.sourceSets.integrationTest.runtimeOnlyConfigurationName().extendsFrom(
        jenkinsPluginHpisAndJpis,
        jenkinsTestLibrariesRuntimeOnly
      )
      jenkinsPlugins.incoming.afterResolve {
        val resolvedArtifacts = jenkinsPlugins.resolvedConfiguration.resolvedArtifacts
        resolvedArtifacts
          .filter { it.extension in setOf("hpi", "jpi") }
          .map { "${it.moduleVersion}@${it.extension}" }
          .forEach { dependencyHandler.add(jenkinsPluginHpisAndJpis, it) }
        // Map each included HPI to that plugin's JAR for usage in compilation of tests
        resolvedArtifacts
          .filter { it.extension in setOf("hpi", "jpi") }
          .map { "${it.moduleVersion}@jar" } // Use the published JAR libraries for each plugin
          .forEach { dependencyHandler.add(jenkinsPluginLibraries, it) }
        // Include all of the additional JAR dependencies from the transitive dependencies of the plugin
        resolvedArtifacts
          .filter { it.extension == "jar" }
          .map { "${it.moduleVersion}@jar" } // TODO: might not need this
          .forEach { dependencyHandler.add(jenkinsPluginLibraries, it) }
      }

      all {
        incoming.beforeResolve {
          if (hierarchy.contains(jenkinsPluginHpisAndJpis)
            || hierarchy.contains(jenkinsPluginLibraries)
          ) {
            // Trigger the dependency seeding
            jenkinsPlugins.resolve()
          }
        }
      }
    }

  }

  /**
   * Creates and sets up a configuraiton for the Groovy dependency.
   */
  private fun Project.setupGroovyConfiguration() {
    val dependencyHandler = dependencies
    configurations {
      GROOVY_CONFIGURATION {
        defaultJenkinsConfigurationSetup()
        withDependencies {
          LOGGER.debug { "Adding ${extensions.sharedLibraryExtension.groovyDependency()} to ${this@GROOVY_CONFIGURATION.name}" }
          dependencyHandler.add(this@GROOVY_CONFIGURATION, extensions.sharedLibraryExtension.groovyDependency())
        }
      }
    }
  }

  private fun Project.setupJenkinsRepository() {
    LOGGER.debug { "Adding repository named $JENKINS_REPOSITORY_NAME with URL $JENKINS_REPOSITORY_URL" }
//    val maven = repositoryHandler.maven(url = JENKINS_REPOSITORY_URL)
//    maven.name = JENKINS_REPOSITORY_NAME
    // Issue with running tests in IntelliJ with this - see https://github.com/gradle/kotlin-dsl/issues/581
    repositories.maven {
      name = JENKINS_REPOSITORY_NAME
      setUrl(JENKINS_REPOSITORY_URL)
    }
  }

  private fun setupJava(
    javaPluginConvention: JavaPluginConvention,
    tasks: TaskContainer
  )  {
    javaPluginConvention.sourceCompatibility = JavaVersion.VERSION_1_8
    javaPluginConvention.targetCompatibility = JavaVersion.VERSION_1_8
    val main by javaPluginConvention.sourceSets.getting {
      java.setSrcDirs(emptyList<String>())
      withConvention(GroovySourceSet::class) { groovy.setSrcDirs(listOf("src", "vars")) }
      resources.setSrcDirs(listOf("resources"))
    }
    val test by javaPluginConvention.sourceSets.getting {
      val unitTestDirectory = "$TEST_ROOT_PATH/unit"
      java.setSrcDirs(listOf("$unitTestDirectory/java"))
      withConvention(GroovySourceSet::class) { groovy.setSrcDirs(listOf("$unitTestDirectory/groovy")) }
      resources.setSrcDirs(listOf("$unitTestDirectory/resources"))
    }
    val generatedIntegrationSources = projectLayout.buildDirectory.dir("generated-src/integrationTest")
    val integrationTest by javaPluginConvention.sourceSets.creating {
      val integrationTestDirectory = "$TEST_ROOT_PATH/integration"
      java.setSrcDirs(listOf("$integrationTestDirectory/java"))
      withConvention(GroovySourceSet::class) {
        groovy.setSrcDirs(listOf("$integrationTestDirectory/groovy", generatedIntegrationSources))
      }
      resources.setSrcDirs(listOf("$integrationTestDirectory/resources"))
      val generateLocalLibraryRetriever by tasks.creating(GenerateJavaFile::class) {
        description = "Generates a LibraryRetriever implementation for easier writing of integration tests"
        srcDir.set(generatedIntegrationSources)
        javaFile.set(localLibraryAdder())
      }
        tasks[getCompileTaskName("groovy")].dependsOn(generateLocalLibraryRetriever)
    }
  }

  private fun Project.createSharedLibraryExtension() {
    val groovyVersion = initializedProperty(DEFAULT_GROOVY_VERSION)
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

    val pluginDependencySpec = PluginDependencySpec(
      workflowApiPluginVersion,
      workflowBasicStepsPluginVersion,
      workflowCpsPluginVersion,
      workflowDurableTaskStepPluginVersion,
      workflowGlobalCpsLibraryPluginVersion,
      workflowJobPluginVersion,
      workflowMultibranchPluginVersion,
      workflowScmStepPluginVersion,
      workflowStepApiPluginVersion,
      workflowSupportPluginVersion
    )
    extensions.create(
      SHARED_LIBRARY_EXTENSION_NAME,
      SharedLibraryExtension::class.java,
      groovyVersion,
      coreVersion,
      pipelineTestUnitVersion,
      testHarnessVersion,
      pluginDependencySpec
    )
  }

  private fun Configuration.defaultJenkinsConfigurationSetup() {
    isCanBeResolved = true
    isVisible = false
    isCanBeConsumed = false
  }

  private val NamedDomainObjectContainerScope<Configuration>.jenkinsPlugins
    get() = getByName(JENKINS_PLUGINS_CONFIGURATION)

  private val ConfigurationContainer.jenkinsPlugins: Configuration
    get() = getByName(JENKINS_PLUGINS_CONFIGURATION)

  private val NamedDomainObjectContainerScope<Configuration>.sharedLibraryGroovy
    get() = getByName(GROOVY_CONFIGURATION)

  private val ConfigurationContainer.sharedLibraryGroovy: Configuration
    get() = getByName(GROOVY_CONFIGURATION)
  
  private val ExtensionContainer.sharedLibraryExtension: SharedLibraryExtension
    get() = getByType(SharedLibraryExtension::class.java)

  private val Project.java: JavaPluginConvention
    get() = withConvention(JavaPluginConvention::class) { this }

  private val SourceSetContainer.main: SourceSet
    get() = getByName("main")

  private val SourceSetContainer.test: SourceSet
    get() = getByName("test")

  private val SourceSetContainer.integrationTest: SourceSet
    get() = maybeCreate("integrationTest")

  private fun DependencyHandler.add(configuration: Configuration, dependencyNotation: Any): Dependency =
    add(configuration.name, dependencyNotation)

  private fun DependencyHandler.createExternal(
    dependencyNotation: Any,
    configuration: ExternalModuleDependency.() -> Unit = {}
  ): ExternalModuleDependency = (this.create(
    dependencyNotation) as ExternalModuleDependency).apply(configuration)
}
