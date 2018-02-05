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
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.GroovySourceSet
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskContainer
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

    // These are internal configurations used in the compilation and runtime
    private const val UNIT_TESTING_LIBRARY_CONFIGURATION = "jenkinsPipelineUnitTestLibraries"
    // These are both unused because we use the kotlin-dsl to create and set them up right now.
    @Suppress("UNUSED")
    private val PLUGIN_HPI_JPI_CONFIGURATION = "jenkinsPluginHpisAndJpis"
    @Suppress("UNUSED")
    private val PLUGIN_LIBRARY_CONFIGURATION = "jenkinsPluginLibraries"
    private const val CORE_LIBRARY_CONFIGURATION = "jenkinsCoreLibraries"
    private const val TEST_LIBRARY_CONFIGURATION = "jenkinsTestLibraries"
    private const val TEST_LIBRARY_RUNTIME_ONLY_CONFIGURATION = "jenkinsTestLibrariesRuntimeOnly"
  }

  override fun apply(target: Project) {
    target.run {
      pluginManager.apply(GroovyPlugin::class.java)
      pluginManager.apply(JenkinsIntegrationPlugin::class.java)
      setupJenkinsRepository(repositories)
      val (main, test, integrationTest) = withConvention(JavaPluginConvention::class) { setupJava(this, tasks) }
      val sharedLibraryExtension = setupSharedLibraryExtension(this)
      setupIntegrationTestTask(tasks, main, integrationTest)
      setupDocumentationTasks(tasks, main)
      setupConfigurationsAndDependencyManagement(
        configurations,
        dependencies,
        main,
        test,
        integrationTest
      )
      afterEvaluate {
        addGroovyDependency(
          dependencies,
          sharedLibraryExtension,
          main
        )
        addDependenciesFromExtension(
          dependencies,
          sharedLibraryExtension
        )
      }
    }
  }

  private fun addDependenciesFromExtension(
    dependencies: DependencyHandler,
    sharedLibraryExtension: SharedLibraryExtension
  ) {
    dependencies.add(
      TEST_LIBRARY_CONFIGURATION,
      sharedLibraryExtension.testHarnessDependency()
    )

    dependencies.add(
      TEST_LIBRARY_RUNTIME_ONLY_CONFIGURATION,
      "${sharedLibraryExtension.jenkinsWar()}@war"
    )

    dependencies.add(
      CORE_LIBRARY_CONFIGURATION,
      sharedLibraryExtension.coreDependency()
    )

    // TODO: remove pluginDependencies().pluginDependencies() confusing method calls
    sharedLibraryExtension.pluginDependencies()
      .pluginDependencies()
      .map { dependencies.createExternal(it.asStringNotation()) }
      .forEach { dependencies.add(JENKINS_PLUGINS_CONFIGURATION, it) }

    dependencies.add(
      UNIT_TESTING_LIBRARY_CONFIGURATION,
      sharedLibraryExtension.pipelineUnitDependency()
    )
  }

  private fun setupDocumentationTasks(tasks: TaskContainer, main: SourceSet) {
    tasks {
      "sourcesJar"(Jar::class) {
        description = "Assemble the sources JAR"
        classifier = "sources"
        from(main.allSource)
      }
      "groovydocJar"(Jar::class) {
        val groovydoc = tasks.getByName(GroovyPlugin.GROOVYDOC_TASK_NAME) as Groovydoc
        dependsOn(groovydoc)
        description = "Assemble the Groovydoc JAR"
        classifier = "javadoc"
      }
    }
  }

  private fun setupIntegrationTestTask(
    tasks: TaskContainer,
    main: SourceSet,
    integrationTestSource: SourceSet
  ) {
    val integrationTest by tasks.creating(Test::class.java) {
      dependsOn(main.classesTaskName)
      mustRunAfter("test")
      group = LifecycleBasePlugin.VERIFICATION_GROUP
      description = "Runs tests with the jenkins-test-harness"
      testClassesDirs = integrationTestSource.output.classesDirs
      classpath = integrationTestSource.runtimeClasspath
      // Set the build directory for Jenkins test harness.
      // See https://issues.jenkins-ci.org/browse/JENKINS-26331
      systemProperty("buildDirectory", projectLayout.buildDirectory.get().asFile.absolutePath)
      shouldRunAfter("test")
    }
    tasks[LifecycleBasePlugin.CHECK_TASK_NAME].dependsOn(integrationTest)
  }

  private fun setupConfigurationsAndDependencyManagement(
    configurations: ConfigurationContainer,
    dependencies: DependencyHandler,
    main: SourceSet,
    test: SourceSet,
    integrationTest: SourceSet
  ) {
    val configurationAction: Configuration.() -> Unit = {
      isCanBeResolved = true
      isVisible = false
      isCanBeConsumed = false
    }

    // TODO: Come up with a better way to collect all the transitive dependencies and HPI/JAR versions of each plugin.
    // Also, forcing dependencies through the extensions does not feel right and is not that intuitive.
    // Instead, it would probably make sense to introduce configurations that users can add additional configuration and dependencies to.
    val jenkinsPlugins by configurations.creating {
      isCanBeResolved = true
    }

    val jenkinsLibrariesMainCompileOnly by configurations.creating
    val jenkinsPluginHpisAndJpis by configurations.creating(configurationAction)
    val jenkinsPluginLibraries by configurations.creating(configurationAction)
    val jenkinsCoreLibraries by configurations.creating(configurationAction)
    val jenkinsTestLibraries by configurations.creating(configurationAction)
    val jenkinsTestLibrariesRuntimeOnly by configurations.creating(configurationAction)
    val jenkinsPipelineUnitTestLibraries by configurations.creating(configurationAction)

    configurations {
      main.compileOnlyConfigurationName().extendsFrom(jenkinsLibrariesMainCompileOnly)
      test.implementationConfigurationName().extendsFrom(jenkinsPipelineUnitTestLibraries)

      integrationTest.implementationConfigurationName().run {
        extendsFrom(
          getByName(main.implementationConfigurationName),
          getByName(test.implementationConfigurationName),
          jenkinsCoreLibraries,
          jenkinsPluginLibraries,
          jenkinsTestLibraries
        )
        exclude(group = "com.lesfurets", module = "jenkins-pipeline-unit")
      }

      integrationTest.runtimeOnlyConfigurationName().extendsFrom(
        jenkinsPluginHpisAndJpis,
        jenkinsTestLibrariesRuntimeOnly
      )
    }

    jenkinsPlugins.incoming.afterResolve {
      val resolvedArtifacts = jenkinsPlugins.resolvedConfiguration.resolvedArtifacts
      resolvedArtifacts
        .filter { it.extension in setOf("hpi", "jpi") }
        .map { "${it.moduleVersion}@${it.extension}" }
        .forEach { dependencies.add(jenkinsPluginHpisAndJpis, it) }
      // Map each included HPI to that plugin's JAR for usage in compilation of tests
      resolvedArtifacts
        .filter { it.extension in setOf("hpi", "jpi") }
        .map { "${it.moduleVersion}@jar" } // Use the published JAR libraries for each plugin
        .forEach { dependencies.add(jenkinsPluginLibraries, it) }
      // Include all of the additional JAR dependencies from the transitive dependencies of the plugin
      resolvedArtifacts
        .filter { it.extension == "jar" }
        .map { "${it.moduleVersion}@jar" } // TODO: might not need this
        .forEach { dependencies.add(jenkinsPluginLibraries, it) }
      // For @NonCPS add the
      resolvedArtifacts
        .filter { it.moduleVersion.id.group == "com.cloudbees" && it.moduleVersion.id.name == "groovy-cps" }
        .forEach { dependencies.add(jenkinsLibrariesMainCompileOnly, it.moduleVersion.toString()) }
    }

    configurations.all {
      incoming.beforeResolve {
        if (hierarchy.contains(jenkinsPluginHpisAndJpis)
          || hierarchy.contains(jenkinsPluginLibraries)
          || hierarchy.contains(jenkinsLibrariesMainCompileOnly)) {
          // Trigger the dependency seeding
          jenkinsPlugins.resolve()
        }
      }
    }
  }

  private fun addGroovyDependency(
    dependencies: DependencyHandler,
    sharedLibrary: SharedLibraryExtension,
    main: SourceSet
  ) {
    LOGGER.debug { "Adding ${sharedLibrary.groovyDependency()} to ${main.implementationConfigurationName}" }
    dependencies.add(
      main.implementationConfigurationName,
      sharedLibrary.groovyDependency()
    )
  }

  private fun setupJenkinsRepository(repositoryHandler: RepositoryHandler) {
    LOGGER.debug { "Adding repository named $JENKINS_REPOSITORY_NAME with URL $JENKINS_REPOSITORY_URL" }
//    val maven = repositoryHandler.maven(url = JENKINS_REPOSITORY_URL)
//    maven.name = JENKINS_REPOSITORY_NAME
    // Issue with running tests in IntelliJ with this - see https://github.com/gradle/kotlin-dsl/issues/581
    repositoryHandler.maven {
      name = JENKINS_REPOSITORY_NAME
      setUrl(JENKINS_REPOSITORY_URL)
    }
  }

  private fun setupJava(
    javaPluginConvention: JavaPluginConvention,
    tasks: TaskContainer
  ): Triple<SourceSet, SourceSet, SourceSet> {
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

    return Triple(main, test, integrationTest)
  }

  private fun setupSharedLibraryExtension(project: Project): SharedLibraryExtension {
    val groovyVersion = project.initializedProperty(DEFAULT_GROOVY_VERSION)
    val coreVersion = project.initializedProperty(DEFAULT_CORE_VERSION)
    val pipelineTestUnitVersion = project.initializedProperty(DEFAULT_JENKINS_PIPELINE_UNIT_VERSION)
    val testHarnessVersion = project.initializedProperty(DEFAULT_TEST_HARNESS_VERSION)
    // TODO: find a better DSL for managing these dependencies, possibly by using aggregator plugin because we are still missing some
    val workflowApiPluginVersion = project.initializedProperty(DEFAULT_WORKFLOW_API_PLUGIN_VERSION)
    val workflowBasicStepsPluginVersion = project.initializedProperty(
      DEFAULT_WORKFLOW_BASIC_STEPS_PLUGIN_VERSION)
    val workflowCpsPluginVersion = project.initializedProperty(DEFAULT_WORKFLOW_CPS_PLUGIN_VERSION)
    val workflowDurableTaskStepPluginVersion = project.initializedProperty(
      DEFAULT_WORKFLOW_DURABLE_TASK_STEP_PLUGIN_VERSION)
    val workflowGlobalCpsLibraryPluginVersion = project.initializedProperty(
      DEFAULT_WORKFLOW_GLOBAL_CPS_LIBRARY_PLUGIN_VERSION)
    val workflowJobPluginVersion = project.initializedProperty(DEFAULT_WORKFLOW_JOB_PLUGIN_VERSION)
    val workflowMultibranchPluginVersion = project.initializedProperty(
      DEFAULT_WORKFLOW_MULTIBRANCH_PLUGIN_VERSION)
    val workflowStepApiPluginVersion = project.initializedProperty(
      DEFAULT_WORKFLOW_STEP_API_PLUGIN_VERSION)
    val workflowScmStepPluginVersion = project.initializedProperty(DEFAULT_WORKFLOW_SCM_STEP_PLUGIN_VERSION)
    val workflowSupportPluginVersion = project.initializedProperty(
      DEFAULT_WORKFLOW_SUPPORT_PLUGIN_VERSION)

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
    return project.extensions.create(
      SHARED_LIBRARY_EXTENSION_NAME,
      SharedLibraryExtension::class.java,
      groovyVersion,
      coreVersion,
      pipelineTestUnitVersion,
      testHarnessVersion,
      pluginDependencySpec
    )
  }

  private fun DependencyHandler.add(configuration: Configuration, dependencyNotation: Any): Dependency =
    add(configuration.name, dependencyNotation)

  private fun DependencyHandler.createExternal(
    dependencyNotation: Any,
    configuration: ExternalModuleDependency.() -> Unit = {}
  ): ExternalModuleDependency = (this.create(
    dependencyNotation) as ExternalModuleDependency).apply(configuration)
}
