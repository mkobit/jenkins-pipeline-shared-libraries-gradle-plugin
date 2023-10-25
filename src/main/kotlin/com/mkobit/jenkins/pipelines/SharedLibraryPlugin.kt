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
// * import easiest to handle https://github.com/gradle/kotlin-dsl/issues/564
import org.gradle.kotlin.dsl.* // ktlint-disable no-wildcard-imports
// @formatter:on
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.util.GradleVersion
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
    private const val DEFAULT_JENKINS_PIPELINE_UNIT_VERSION = "1.5"
    private const val DEFAULT_CORE_VERSION = "2.222.4"
    private const val DEFAULT_TEST_HARNESS_VERSION = "2.64"
    private const val DEFAULT_WORKFLOW_API_PLUGIN_VERSION = "2.40"
    private const val DEFAULT_WORKFLOW_BASIC_STEPS_PLUGIN_VERSION = "2.20"
    private const val DEFAULT_WORKFLOW_CPS_PLUGIN_VERSION = "2.80"
    private const val DEFAULT_WORKFLOW_DURABLE_TASK_STEP_PLUGIN_VERSION = "2.35"
    private const val DEFAULT_WORKFLOW_GLOBAL_CPS_LIBRARY_PLUGIN_VERSION = "2.16"
    private const val DEFAULT_WORKFLOW_JOB_PLUGIN_VERSION = "2.39"
    private const val DEFAULT_WORKFLOW_MULTIBRANCH_PLUGIN_VERSION = "2.21"
    private const val DEFAULT_WORKFLOW_STEP_API_PLUGIN_VERSION = "2.22"
    private const val DEFAULT_WORKFLOW_SCM_STEP_PLUGIN_VERSION = "2.11"
    private const val DEFAULT_WORKFLOW_SUPPORT_PLUGIN_VERSION = "3.5"

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
    private const val WAR_CONFIGURATION = "jenkinsWar"
    private const val ONLY_WAR_CONFIGURATION = "jenkinsOnlyWarExtension"
    private const val MODULES_CONFIGURATION = "jenkinsModules"

    private const val IVY_CONFIGURATION = "sharedLibraryIvy"
    private const val IVY_COORDINATES = "org.apache.ivy:ivy:2.4.0"
  }

  override fun apply(target: Project) {
    target.run {
      // Ordering of calls here matters
      apply<GroovyPlugin>()
      apply<JenkinsIntegrationPlugin>()
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
    val main = sourceSets.main.apply {
      java.setSrcDirs(emptyList<String>())
      groovy.setSrcDirs(listOf("src", "vars"))
      resources.setSrcDirs(listOf("resources"))
    }

    configurations {
      main.implementationConfigurationName {
        this.extendsFrom(
          GROOVY_CONFIGURATION().get(),
          CORE_LIBRARY_CONFIGURATION().get(),
          PLUGIN_LIBRARY_CONFIGURATION().get()
        )
      }
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
      val unitTestLibrary = create(UNIT_TESTING_LIBRARY_CONFIGURATION) {
        description = "Jenkins Pipeline Unit libraries"
        defaultJenkinsConfigurationSetup()
        withDependencies {
          LOGGER.debug { "Adding JenkinsPipelineUnit dependency to configuration${this@create.name}" }
          dependencyHandler.add(this@create, sharedLibraryExtension.pipelineUnitDependency().get())
        }
      }
      getByName(test.implementationConfigurationName) {
        extendsFrom(unitTestLibrary, getByName(GROOVY_CONFIGURATION))
      }
    }
  }

  /**
   * Sets up all configurations and dependencies.
   */
  private fun Project.setupConfigurationsAndDependencies() {
    val dependencyHandler = dependencies
    val sharedLibraryExtension = sharedLibraryExtension
    configurations {
      val jenkinsPlugins = create(JENKINS_PLUGINS_CONFIGURATION) {
        defaultJenkinsConfigurationSetup(canBeResolved = true)
        description = "Jenkins plugin dependencies"
        withDependencies {
          LOGGER.debug { "Adding plugin dependencies from ${SharedLibraryExtension::class.java.canonicalName} to configuration $name" }
          // TODO: remove pluginDependencies().pluginDependencies() confusing method calls
          sharedLibraryExtension.pluginDependencies()
            .pluginDependencies()
            .get()
            .map { it.asStringNotation() }
            .onEach { LOGGER.debug { "Adding plugin dependency notation $it to configuration $name" } }
            .forEach { dependencyHandler.add(this@create, it) }
        }
      }
      create(PLUGIN_HPI_JPI_CONFIGURATION) {
        defaultJenkinsConfigurationSetup()
        description = "Jenkins plugin HPI and JPI dependencies needed at runtime"
        withDependencies {
          jenkinsPlugins.resolvedConfiguration.resolvedArtifacts
            .filter { it.extension in setOf("hpi", "jpi") }
            .onEach { LOGGER.debug { "Adding plugin dependency $it with extension ${it.extension} to configuration $name" } }
            .map { "${it.moduleVersion}@${it.extension}" }
            .forEach { dependencyHandler.add(this@create, it) }
        }
      }
      create(PLUGIN_LIBRARY_CONFIGURATION) {
        defaultJenkinsConfigurationSetup()
        description = "Jenkins plugin JAR libraries"
        withDependencies {
          // Map each included HPI to that plugin's JAR for usage in compilation of tests and also
          // include all of the additional JAR dependencies from the transitive dependencies of the plugin
          jenkinsPlugins.resolvedConfiguration.resolvedArtifacts
            .filter { it.extension in setOf("hpi", "jpi", "jar") }
            .onEach { LOGGER.debug { "Adding JAR dependency on $it to configuration $name" } }
            .map { "${it.moduleVersion}@jar" } // Use the published JAR libraries for each plugin
            .forEach { dependencyHandler.add(this@create, it) }

          // TODO: hack - total hack to get the stupid servlet API dependency on the classpath
          // since I can't seem to pull it from the POM of Jenkins Core due to it being 'provided' scope
          // and I don't know where it transitively comes from to determine it as dependency resolution time.
          // See https://discuss.gradle.org/t/how-to-query-for-provided-scoped-dependencies-of-a-configuration/26131
          // This probably won't work for older versions of Jenkins because of different servlet-api
          // versions, but should work enough for now to enable usage of Jenkins core and plugin classes in the library
          // source
          dependencyHandler.add(this@create, "javax.servlet:javax.servlet-api:3.1.0")
        }
      }
      val coreLibrary = create(CORE_LIBRARY_CONFIGURATION) {
        defaultJenkinsConfigurationSetup(canBeResolved = true)
        description = "Jenkins core libraries and modules"
        withDependencies {
          dependencyHandler.add(this@create, sharedLibraryExtension.coreDependency().get())
        }
      }
      val jenkinsWar = create(WAR_CONFIGURATION) {
        defaultJenkinsConfigurationSetup(canBeResolved = true)
        description = "Jenkins WAR libraries and modules"
        withDependencies {
          dependencyHandler.add(this@create, sharedLibraryExtension.jenkinsWar().get())
        }
      }
      create(ONLY_WAR_CONFIGURATION) {
        defaultJenkinsConfigurationSetup()
        description = "Contains the WAR distribution of Jenkins"
        withDependencies {
          jenkinsWar.resolvedConfiguration.resolvedArtifacts
            .filter { it.extension == "war" }
            .onEach { LOGGER.debug { "Adding WAR dependency $it to configuration $name" } }
            .forEach { dependencyHandler.add(this@create, "${it.moduleVersion}@war") }
        }
      }
      create(MODULES_CONFIGURATION) {
        defaultJenkinsConfigurationSetup(canBeResolved = true)
        description = "Jenkins WAR libraries and modules"
        withDependencies {
          jenkinsWar.resolvedConfiguration.resolvedArtifacts
            .filter { it.moduleVersion.id.group == "org.jenkins-ci.modules" }
            .filter { it.extension == "jar" }
            .onEach { LOGGER.debug { "Adding module dependency $it to configuration $name" } }
            .forEach { dependencyHandler.add(this@create, it.moduleVersion.toString()) }
        }
      }
      create(TEST_LIBRARY_CONFIGURATION) {
        defaultJenkinsConfigurationSetup()
        description = "Jenkins test harness and modules"
        withDependencies {
          dependencyHandler.add(this@create, sharedLibraryExtension.testHarnessDependency().get())
        }
      }
      create(TEST_LIBRARY_RUNTIME_ONLY_CONFIGURATION) {
        defaultJenkinsConfigurationSetup()
        description = "Jenkins test runtime libraries"
        withDependencies {
          sharedLibraryExtension.jenkinsWar().map { "$it@war" }
          dependencyHandler.add(this@create, sharedLibraryExtension.jenkinsWar().map { "$it@war" }.get())
        }
      }
      create(GROOVY_CONFIGURATION) {
        defaultJenkinsConfigurationSetup()
        description = "Shared Library Groovy dependency"
        withDependencies {
          val groovyArtifact = coreLibrary.resolvedConfiguration.resolvedArtifacts.find {
            it.moduleVersion.id.group == "org.codehaus.groovy"
          } ?: throw GradleException("Could not find Groovy dependency from Jenkins core")
          LOGGER.debug { "Adding ${groovyArtifact.moduleVersion} to configuration $name" }
          dependencyHandler.add(this@create, groovyArtifact.moduleVersion.id.toString())
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
      withType<GroovyCompile>().configureEach {
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
    val hasArchiveClassifier = GradleVersion.current() >= GradleVersion.version("5.1")
    tasks {
      register<Jar>("sourcesJar") {
        description = "Creates a JAR of the source"
        description = "Assemble the sources JAR"
        if (hasArchiveClassifier) {
          archiveClassifier.set("sources")
        } else {
          classifier = "sources"
        }
        from(java.sourceSets.main.allSource)
      }
      val groovydoc = named<Groovydoc>(GroovyPlugin.GROOVYDOC_TASK_NAME)
      register<Jar>("groovydocJar") {
        description = "Creates a JAR of the Groovydoc"
        dependsOn(groovydoc)
        description = "Assemble the Groovydoc JAR"
        if (hasArchiveClassifier) {
          archiveClassifier.set("javadoc")
        } else {
          classifier = "javadoc"
        }
      }
    }
  }

  /**
   * Creates and sets up the `integrationTest` source set.
   */
  private fun Project.setupIntegrationTest() {
    val generatedIntegrationDir = projectLayout.buildDirectory.dir("generated-src/integrationTest")
    val generatedIntegrationSource = generatedIntegrationDir.map { it.dir("src") }
    val generatedIntegrationResources = generatedIntegrationDir.map { it.dir("resources") }
    val integrationTestSourceSet = java.sourceSets.create(INTEGRATION_TEST_SOURCE_SET) {
      description = "Integration test set for shared library source"
      val integrationTestDirectory = "$TEST_ROOT_PATH/integration"
      java.setSrcDirs(listOf("$integrationTestDirectory/java"))
      groovy.setSrcDirs(listOf("$integrationTestDirectory/groovy", generatedIntegrationSource))
      resources.setSrcDirs(listOf("$integrationTestDirectory/resources", generatedIntegrationResources))
    }

    configurations {
      sourceSets.integrationTest.implementationConfigurationName {
        extendsFrom(
          getByName(GROOVY_CONFIGURATION),
          getByName(CORE_LIBRARY_CONFIGURATION),
          getByName(PLUGIN_LIBRARY_CONFIGURATION),
          getByName(TEST_LIBRARY_CONFIGURATION),
          getByName(MODULES_CONFIGURATION)
        )
      }

      sourceSets.integrationTest.runtimeOnlyConfigurationName {
        extendsFrom(
          getByName(PLUGIN_HPI_JPI_CONFIGURATION),
          getByName(TEST_LIBRARY_RUNTIME_ONLY_CONFIGURATION),
          getByName(ONLY_WAR_CONFIGURATION)
        )
      }
    }

    tasks {
      val generateGeneratedAnnotation by registering(GenerateJavaFile::class) {
        description = "Generates the com.mkobit.jenkins.pipelines.codegen.JenkinsGradlePluginSharedLibraryGenerated marker annotation for code generated by the Jenkins Shared Library Gradle plugin"
        srcDir.set(generatedIntegrationSource)
        javaFile.set(internalGeneratedAnnotation())
      }
      val generateLocalLibraryRetriever by registering(GenerateJavaFile::class) {
        description = "Generates an org.jenkinsci.plugins.workflow.libs.LibraryRetriever implementation for simplified integration with @JenkinsRule and local source code"
        srcDir.set(generatedIntegrationSource)
        javaFile.set(localLibraryAdder())
      }
      val generateIntegrationClassFilter by registering {
        description = "Generates a class filter (https://jenkins.io/blog/2018/01/13/jep-200/)"
        val resourceFile = generatedIntegrationResources.map { it.file("META-INF/hudson.remoting.ClassFilter") }
        outputs.file(resourceFile)
        val resourceFileContent =
          """
            # Generated by Shared Library Gradle Plugin
            com.mkobit.jenkins.pipelines.codegen.LocalLibraryRetriever
          """.trimIndent()
        inputs.property("content", resourceFileContent)
        doFirst {
          resourceFile.get().asFile.run {
            parentFile.mkdirs()
            writeText(resourceFileContent, Charsets.UTF_8)
          }
        }
      }
      named(integrationTestSourceSet.getCompileTaskName("groovy")) {
        dependsOn(generateLocalLibraryRetriever, generateGeneratedAnnotation)
      }
      named(integrationTestSourceSet.processResourcesTaskName) {
        dependsOn(generateIntegrationClassFilter)
      }
      val integrationTest = register<Test>(INTEGRATION_TEST_TASK) {
        dependsOn(java.sourceSets.main.classesTaskName)
        mustRunAfter("test")
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Runs tests with the jenkins-test-harness"
        testClassesDirs = sourceSets.integrationTest.output.classesDirs
        classpath = sourceSets.integrationTest.runtimeClasspath
        // Set the build directory for Jenkins test harness.
        // See https://issues.jenkins-ci.org/browse/JENKINS-26331
        systemProperty("buildDirectory", projectLayout.buildDirectory.get().asFile.absolutePath)
        shouldRunAfter("test")
      }
      named(LifecycleBasePlugin.CHECK_TASK_NAME) {
        dependsOn(integrationTest)
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
      DEFAULT_WORKFLOW_BASIC_STEPS_PLUGIN_VERSION
    )
    val workflowCpsPluginVersion = initializedProperty(DEFAULT_WORKFLOW_CPS_PLUGIN_VERSION)
    val workflowDurableTaskStepPluginVersion = initializedProperty(
      DEFAULT_WORKFLOW_DURABLE_TASK_STEP_PLUGIN_VERSION
    )
    val workflowGlobalCpsLibraryPluginVersion = initializedProperty(
      DEFAULT_WORKFLOW_GLOBAL_CPS_LIBRARY_PLUGIN_VERSION
    )
    val workflowJobPluginVersion = initializedProperty(DEFAULT_WORKFLOW_JOB_PLUGIN_VERSION)
    val workflowMultibranchPluginVersion = initializedProperty(DEFAULT_WORKFLOW_MULTIBRANCH_PLUGIN_VERSION)
    val workflowStepApiPluginVersion = initializedProperty(DEFAULT_WORKFLOW_STEP_API_PLUGIN_VERSION)
    val workflowScmStepPluginVersion = initializedProperty(DEFAULT_WORKFLOW_SCM_STEP_PLUGIN_VERSION)
    val workflowSupportPluginVersion = initializedProperty(DEFAULT_WORKFLOW_SUPPORT_PLUGIN_VERSION)

    val pluginDependencySpec = objectFactory.newInstance(
      PluginDependencySpec::class,
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
      SharedLibraryExtension::class,
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
   * Gets the extension of type [SharedLibraryExtension].
   */
  private val Project.sharedLibraryExtension: SharedLibraryExtension
    get() = the()

  /**
   * Gets the [JavaPluginConvention].
   */
  private val Project.java: JavaPluginConvention
    get() = the()

  /**
   * Gets the [SourceSetContainer].
   */
  private val Project.sourceSets: SourceSetContainer
    get() = the()

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
    add(configuration.name, dependencyNotation)!!
}
