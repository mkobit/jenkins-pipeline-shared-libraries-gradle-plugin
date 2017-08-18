package com.mkobit.jenkins.pipelines

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.file.ProjectLayout
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPluginConvention
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
    internal val PLUGIN_HPI_CONFIGURATION = "pluginJpiDependencies"
  }

  override fun apply(project: Project) {
    project.pluginManager.apply(GroovyPlugin::class.java)
    setupJenkinsRepository(project.repositories)
    val (main, test, integrationTest) = setupJava(project.convention.getPlugin(JavaPluginConvention::class.java))
    val sharedLibraryExtension = createSharedLibraryExtension(project)
    setupIntegrationTestTask(project.tasks, main, integrationTest)
    setupDocumentationTasks(project.tasks, main)
    setupPluginHpiConfiguration(
      project.configurations,
      main,
      test,
      integrationTest
    )
    project.afterEvaluate {
      setupGroovyDependency(project.dependencies, sharedLibraryExtension, main)
      setupIntegrationTestDependencies(project.configurations, project.dependencies, sharedLibraryExtension, integrationTest)
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

  private fun setupIntegrationTestDependencies(
    configurations: ConfigurationContainer,
    dependencies: DependencyHandler,
    sharedLibraryExtension: SharedLibraryExtension,
    integrationTest: SourceSet
  ) {
    dependencies.add(
      integrationTest.implementationConfigurationName,
      jenkinsTestHarnessDependency(sharedLibraryExtension.testHarnessVersion)
    )

    dependencies.add(
      integrationTest.runtimeOnlyConfigurationName,
      jenkinsWar(sharedLibraryExtension.coreVersion)
    )

    dependencies.add(
      integrationTest.implementationConfigurationName,
      jenkinsCoreDependency(sharedLibraryExtension.coreVersion)
    )
  }

  private fun setupPluginHpiConfiguration(
    configurations: ConfigurationContainer,
    main: SourceSet,
    test: SourceSet,
    integrationTest: SourceSet
  ) {
    val pluginHpiConfiguration = configurations.create(PLUGIN_HPI_CONFIGURATION) {
      it.apply {
        isCanBeResolved = true
        isVisible = false
      }
    }
    configurations.getByName(integrationTest.implementationConfigurationName).extendsFrom(
      configurations.getByName(main.implementationConfigurationName),
      configurations.getByName(test.implementationConfigurationName)
    )
    configurations.getByName(integrationTest.runtimeOnlyConfigurationName).extendsFrom(pluginHpiConfiguration)
  }

  private fun setupGroovyDependency(
    dependencies: DependencyHandler,
    sharedLibrary: SharedLibraryExtension,
    main: SourceSet
  ) {
    dependencies.add(
      main.implementationConfigurationName,
      groovyDependency(sharedLibrary.groovyVersion)
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
      groovy.setSrcDirs(listOf("src", "vars"))
      resources.setSrcDirs(listOf("resources"))
    }
    val test = javaPluginConvention.sourceSets.getByName("test").apply {
      val unitTestDirectory = "$TEST_ROOT_PATH/unit"
      java.setSrcDirs(listOf("$unitTestDirectory/java"))
      groovy.setSrcDirs(listOf("$unitTestDirectory/groovy"))
      resources.setSrcDirs(listOf("$unitTestDirectory/resources"))
    }
    val integrationTest = javaPluginConvention.sourceSets.create("integrationTest") {
      it.apply {
        val integrationTestDirectory = "$TEST_ROOT_PATH/integration"
        java.setSrcDirs(listOf("$integrationTestDirectory/java"))
        groovy.setSrcDirs(listOf("$integrationTestDirectory/groovy"))
        resources.setSrcDirs(listOf("$integrationTestDirectory/resources"))
      }
    }

    return Triple(main, test, integrationTest)
  }

  private fun createSharedLibraryExtension(project: Project): SharedLibraryExtension {
    val groovyVersion = project.initializedProperty(DEFAULT_GROOVY_VERSION)
    val coreVersion = project.initializedProperty(DEFAULT_CORE_VERSION)
    val globalLibPluginVersion = project.initializedProperty(DEFAULT_GLOBAL_LIB_PLUGIN_VERSION)
    val testHarnessVersion = project.initializedProperty(DEFAULT_TEST_HARNESS_VERSION)
    return project.extensions.create(
      "sharedLibrary",
      SharedLibraryExtension::class.java,
      groovyVersion,
      coreVersion,
      globalLibPluginVersion,
      testHarnessVersion
    )
  }

  private fun jenkinsCoreDependency(version: String) = "org.jenkins-ci.main:jenkins-core:$version"
  private fun groovyDependency(version: String) = "org.codehaus.groovy:groovy:$version"
  private fun jenkinsTestHarnessDependency(version: String) = "org.jenkins-ci.main:jenkins-test-harness:$version"
  // See https://issues.jenkins-ci.org/browse/JENKINS-24064 and 2.64 release notes about war-for-test not being needed in some cases
  // Also, see https://github.com/jenkinsci/jenkins/pull/2899/files
  // https://github.com/jenkinsci/plugin-pom/pull/40/files shows how the new plugin-pom does the jenkins.war generation
  private fun jenkinsWar(version: String) = "org.jenkins-ci.main:jenkins-war:$version@war"
}
