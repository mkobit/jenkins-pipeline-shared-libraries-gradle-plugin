package com.mkobit.jenkins.pipelines

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.file.ProjectLayout
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.GroovySourceSet
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.javadoc.Groovydoc
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.language.base.plugins.LifecycleBasePlugin
import javax.inject.Inject

open class SharedLibraryPlugin @Inject constructor(
  private val projectLayout: ProjectLayout
) : Plugin<Project> {

  companion object {
    val JENKINS_REPOSITORY_NAME = "JenkinsPublic"
    val JENKINS_REPOSITORY_URL = "https://repo.jenkins-ci.org/public/"
    private val TEST_ROOT_PATH = "test"
    private val DEFAULT_GROOVY_VERSION = "2.4.8"
    private val DEFAULT_CORE_VERSION = "2.60.2"
    private val DEFAULT_GLOBAL_LIB_PLUGIN_VERSION = "2.8"
    private val DEFAULT_TEST_HARNESS_VERSION = "2.24"
    private val UNIT_TESTING_LIBRARY_CONFIGURATION = "jenkinsPipelineUnitTestLibraries"
    private val PLUGIN_HPI_JPI_CONFIGURATION = "jenkinsPluginHpisAndJpis"
    private val PLUGIN_LIBRARY_CONFIGURATION = "jenkinsPluginLibraries"
    private val CORE_LIBRARY_CONFIGURATION = "jenkinsCoreLibraries"
    private val TEST_LIBRARY_CONFIGURATION = "jenkinsTestLibraries"
    private val TEST_LIBRARY_RUNTIME_ONLY_CONFIGURATION = "jenkinsTestLibrariesRuntimeOnly"
  }

  override fun apply(project: Project) {
    project.pluginManager.apply(GroovyPlugin::class.java)
    setupJenkinsRepository(project.repositories)
    val (main, test, integrationTest) = setupJava(project.convention.getPlugin(JavaPluginConvention::class.java))
    val sharedLibraryExtension = setupSharedLibraryExtension(project)
    setupIntegrationTestTask(project.tasks, main, integrationTest)
    setupDocumentationTasks(project.tasks, main)
    setupConfigurations(
      project.configurations,
      main,
      test,
      integrationTest
    )
    project.afterEvaluate {
      setupGroovyDependency(project.dependencies, sharedLibraryExtension, main)
      setupDependencies(project.configurations, project.dependencies, sharedLibraryExtension)
    }
  }

  private fun setupDependencies(
    configurations: ConfigurationContainer,
    dependencies: DependencyHandler,
    sharedLibraryExtension: SharedLibraryExtension
  ) {
    dependencies.add(
      TEST_LIBRARY_CONFIGURATION,
      sharedLibraryExtension.jenkinsTestHarnessDependency()
    )

    dependencies.add(
      TEST_LIBRARY_RUNTIME_ONLY_CONFIGURATION,
      "${sharedLibraryExtension.jenkinsWar()}@war"
    )

    dependencies.add(
      CORE_LIBRARY_CONFIGURATION,
      sharedLibraryExtension.jenkinsCoreDependency()
    )

    sharedLibraryExtension.pluginDependencies().forEach {
      // TODO: when kotlin-dsl works in IntelliJ switch to it
      // TODO: figure out how to get transitive plugin management working
      val hpiDependency = dependencies.createExternal(it.asStringNotation())
//      val jarDependency = (dependencies.create("${it.asStringNotation()}@jar") as ExternalModuleDependency)

      dependencies.add(
        PLUGIN_HPI_JPI_CONFIGURATION,
        hpiDependency
      )
//      dependencies.add(
//        PLUGIN_LIBRARY_CONFIGURATION,
//        jarDependency
//      )
    }

    // TODO: don't resolve configurations early if we don't have to.
    // We do need access to the transitive dependencies to get all of the HPIs and JAR libraries in code completion and I haven't thought of a better way of handling it yet.
    configurations.getByName(PLUGIN_LIBRARY_CONFIGURATION).incoming.beforeResolve {
      configurations.getByName(PLUGIN_HPI_JPI_CONFIGURATION).resolvedConfiguration.resolvedArtifacts.filter {
        it.extension.endsWith(".hpi") || it.extension.endsWith(".jpi")
      }.forEach {
        dependencies.add(
          PLUGIN_LIBRARY_CONFIGURATION,
          dependencies.createExternal("${it.moduleVersion}@jar")
        )
      }
    }

    sharedLibraryExtension.jenkinsPipelineUnitDependency()?.let {
      dependencies.add(
        UNIT_TESTING_LIBRARY_CONFIGURATION,
        it
      )
    }
  }

  private fun setupDocumentationTasks(tasks: TaskContainer, main: SourceSet) {
    tasks.create("sourcesJar", Jar::class.java) {
      it.apply {
        description = "Assemble the sources JAR"
        classifier = "sources"
        from(main.allSource)
      }
    }

    tasks.create("groovydocJar", Jar::class.java) {
      it.apply {
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
    integrationTest: SourceSet
  ) {
    tasks.create("integrationTest", Test::class.java) {
      it.apply {
        dependsOn(main.classesTaskName)
        mustRunAfter("test")
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Runs tests with the jenkins-test-harness"
        testClassesDirs = integrationTest.output.classesDirs
        classpath = integrationTest.runtimeClasspath
        // Set the build directory for Jenkins test harness.
        // See https://issues.jenkins-ci.org/browse/JENKINS-26331
        systemProperty("buildDirectory", projectLayout.buildDirectory.get().get().absolutePath)
      }
    }
  }

  private fun setupConfigurations(
    configurations: ConfigurationContainer,
    main: SourceSet,
    test: SourceSet,
    integrationTest: SourceSet
  ) {
    val configurationAction: (Configuration) -> Unit = {
      it.apply {
        isCanBeResolved = true
        isVisible = false
      }
    }
    val pluginHpiAndJpi = configurations.create(PLUGIN_HPI_JPI_CONFIGURATION, configurationAction)
    val pluginLibraries = configurations.create(PLUGIN_LIBRARY_CONFIGURATION, configurationAction)
    val coreLibraries = configurations.create(CORE_LIBRARY_CONFIGURATION, configurationAction)
    val testLibrary = configurations.create(TEST_LIBRARY_CONFIGURATION, configurationAction)
    val testLibraryRuntimeOnly = configurations.create(TEST_LIBRARY_RUNTIME_ONLY_CONFIGURATION, configurationAction)
    val unitTestingLibraries = configurations.create(UNIT_TESTING_LIBRARY_CONFIGURATION, configurationAction)

    configurations.getByName(test.implementationConfigurationName).extendsFrom(unitTestingLibraries)

    configurations.getByName(integrationTest.implementationConfigurationName).extendsFrom(
      configurations.getByName(main.implementationConfigurationName),
      configurations.getByName(test.implementationConfigurationName),
      coreLibraries,
      pluginLibraries,
      testLibrary
    )
    configurations.getByName(integrationTest.runtimeOnlyConfigurationName).extendsFrom(
      pluginHpiAndJpi,
      testLibraryRuntimeOnly
    )
  }

  private fun setupGroovyDependency(
    dependencies: DependencyHandler,
    sharedLibrary: SharedLibraryExtension,
    main: SourceSet
  ) {
    dependencies.add(
      main.implementationConfigurationName,
      sharedLibrary.groovyDependency()
    )
  }

  private fun setupJenkinsRepository(repositoryHandler: RepositoryHandler) {
    repositoryHandler.maven {
      it.name = JENKINS_REPOSITORY_NAME
      it.setUrl(JENKINS_REPOSITORY_URL)
    }
  }

  private fun setupJava(
    javaPluginConvention: JavaPluginConvention
  ): Triple<SourceSet, SourceSet, SourceSet> {
    javaPluginConvention.sourceCompatibility = JavaVersion.VERSION_1_8
    javaPluginConvention.targetCompatibility = JavaVersion.VERSION_1_8
    val main = javaPluginConvention.sourceSets.getByName("main").apply {
      java.setSrcDirs(emptyList<String>())
      (this as HasConvention).convention.getPlugin(GroovySourceSet::class.java).groovy.setSrcDirs(listOf("src", "vars"))
      resources.setSrcDirs(listOf("resources"))
    }
    val test = javaPluginConvention.sourceSets.getByName("test").apply {
      val unitTestDirectory = "$TEST_ROOT_PATH/unit"
      java.setSrcDirs(listOf("$unitTestDirectory/java"))
      (this as HasConvention).convention.getPlugin(GroovySourceSet::class.java).groovy.setSrcDirs(listOf("$unitTestDirectory/groovy"))
      resources.setSrcDirs(listOf("$unitTestDirectory/resources"))
    }
    val integrationTest = javaPluginConvention.sourceSets.create("integrationTest") {
      it.apply {
        val integrationTestDirectory = "$TEST_ROOT_PATH/integration"
        java.setSrcDirs(listOf("$integrationTestDirectory/java"))
        (this as HasConvention).convention.getPlugin(GroovySourceSet::class.java).groovy.setSrcDirs(listOf("$integrationTestDirectory/groovy"))
        resources.setSrcDirs(listOf("$integrationTestDirectory/resources"))
      }
    }

    return Triple(main, test, integrationTest)
  }

  private fun setupSharedLibraryExtension(project: Project): SharedLibraryExtension {
    val groovyVersion = project.initializedProperty(DEFAULT_GROOVY_VERSION)
    val coreVersion = project.initializedProperty(DEFAULT_CORE_VERSION)
    val globalLibPluginVersion = project.initializedProperty(DEFAULT_GLOBAL_LIB_PLUGIN_VERSION)
    val testHarnessVersion = project.initializedProperty(DEFAULT_TEST_HARNESS_VERSION)
    val pipelineTestUnitVersion = project.property(String::class.java)
    return project.extensions.create(
      "sharedLibrary",
      SharedLibraryExtension::class.java,
      groovyVersion,
      coreVersion,
      globalLibPluginVersion,
      testHarnessVersion,
      pipelineTestUnitVersion
    )
  }

  private fun DependencyHandler.createExternal(
    dependencyNotation: Any,
    configuration: ExternalModuleDependency.() -> Unit = {}
  ): ExternalModuleDependency = (this.create(
    dependencyNotation) as ExternalModuleDependency).apply(configuration)
}
