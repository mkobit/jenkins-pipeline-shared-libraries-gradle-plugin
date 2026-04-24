package com.mkobit.jenkins.pipelines

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.types.shouldBeInstanceOf
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.GroovySourceDirectorySet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import testsupport.junit.Issue
import testsupport.junit.NotImplementedYet

internal class SharedLibraryPluginTest {
  private lateinit var project: Project

  @BeforeEach
  internal fun setUp() {
    project = ProjectBuilder.builder().build()
    project.pluginManager.apply(SharedLibraryPlugin::class.java)
  }

  @Test
  internal fun `Groovy plugin is applied`() {
    project.pluginManager.hasPlugin("groovy") shouldBe true
  }

  @Test
  internal fun `JenkinsIntegrationPlugin is applied`() {
    project.plugins.hasPlugin(JenkinsIntegrationPlugin::class.java) shouldBe true
  }

  @Test
  internal fun `src is a Groovy source directory`() {
    val main = project.extensions.getByType(SourceSetContainer::class.java).getByName("main")
    val srcDirs = main.extensions.getByType(GroovySourceDirectorySet::class.java).srcDirs
    srcDirs.map { it.name }.shouldContain("src")
  }

  @Test
  internal fun `vars is a Groovy source directory`() {
    val main = project.extensions.getByType(SourceSetContainer::class.java).getByName("main")
    val srcDirs = main.extensions.getByType(GroovySourceDirectorySet::class.java).srcDirs
    srcDirs.map { it.name }.shouldContain("vars")
  }

  @Test
  internal fun `resources is the only resources source directory`() {
    val main = project.extensions.getByType(SourceSetContainer::class.java).getByName("main")
    val srcDirs = main.resources.srcDirs
    srcDirs shouldHaveSize 1
    srcDirs.first().name shouldBe "resources"
  }

  @Test
  internal fun `main has no Java sources`() {
    val main = project.extensions.getByType(SourceSetContainer::class.java).getByName("main")
    main.java.srcDirs.shouldBeEmpty()
  }

  @Test
  internal fun `integrationTest task is in the verification group`() {
    val task = project.tasks.getByName("integrationTest")
    task.shouldBeInstanceOf<org.gradle.api.tasks.testing.Test>()
    task.group shouldBe JavaBasePlugin.VERIFICATION_GROUP
    task.description.shouldNotBeBlank()
  }

  @Test
  internal fun `groovydocJar task is created`() {
    val task = project.tasks.getByName("groovydocJar")
    task.shouldBeInstanceOf<Jar>()
    task.description.shouldNotBeBlank()
  }

  @Test
  internal fun `sourcesJar task is created`() {
    val task = project.tasks.getByName("sourcesJar")
    task.shouldBeInstanceOf<Jar>()
    task.description.shouldNotBeBlank()
  }

  @TestFactory
  internal fun `internal configurations are not visible`(): List<DynamicTest> {
    val internalConfigurations =
      listOf(
        "jenkinsPluginClasspath" to "Jenkins plugin JARs + jenkins-core for shared library compilation and unit tests",
        "jenkinsPluginHpis" to "Jenkins plugin HPI archives for embedded Jenkins runtime (integration tests)",
        "jenkinsPipelineUnit" to "JenkinsPipelineUnit library for shared library unit tests",
        "jenkinsTestHarness" to "jenkins-test-harness and its transitive dependencies",
        "sharedLibraryIvy" to "Ivy for @Grab support in shared library Groovy sources",
      )
    return internalConfigurations.map { (name, _) ->
      dynamicTest("configuration '$name' is not visible and has a description") {
        val config = project.configurations.getByName(name)
        config.description.shouldNotBeNull().shouldNotBeBlank()
        config.isVisible.shouldBeFalse()
      }
    }
  }

  @Test
  @Issue("https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/101")
  internal fun `jenkinsPlugin configuration is user-facing and can be declared on`() {
    val config = project.configurations.getByName("jenkinsPlugin")
    config.isCanBeResolved shouldBe false
    config.isCanBeConsumed shouldBe false
    config.description.shouldNotBeNull().shouldNotBeBlank()
  }

  @NotImplementedYet
  @Test
  internal fun `Jenkins Global Library plugin implementation and HPI dependencies are added`() {}

  @NotImplementedYet
  @Test
  internal fun `additional resources directory available for main to be able to use the Jenkins GDSL in IntelliJ`() {}

  @NotImplementedYet
  @Test
  internal fun `integrationTestPipelineResources directory is a source set and available on integrationRuntimeOnly classpath`() {}
}
