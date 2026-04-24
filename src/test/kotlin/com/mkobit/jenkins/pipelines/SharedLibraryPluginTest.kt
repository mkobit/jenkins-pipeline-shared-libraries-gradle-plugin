package com.mkobit.jenkins.pipelines

import io.kotest.core.spec.style.DescribeSpec
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

internal class SharedLibraryPluginTest : DescribeSpec({
  lateinit var project: Project

  beforeTest {
    project = ProjectBuilder.builder().build()
    project.pluginManager.apply(SharedLibraryPlugin::class.java)
  }

  it("applies the Groovy plugin") {
    project.pluginManager.hasPlugin("groovy") shouldBe true
  }

  it("applies JenkinsIntegrationPlugin") {
    project.plugins.hasPlugin(JenkinsIntegrationPlugin::class.java) shouldBe true
  }

  describe("main source set") {
    it("includes src as a Groovy source directory") {
      val main = project.extensions.getByType(SourceSetContainer::class.java).getByName("main")
      main.extensions.getByType(GroovySourceDirectorySet::class.java).srcDirs
        .map { it.name }.shouldContain("src")
    }

    it("includes vars as a Groovy source directory") {
      val main = project.extensions.getByType(SourceSetContainer::class.java).getByName("main")
      main.extensions.getByType(GroovySourceDirectorySet::class.java).srcDirs
        .map { it.name }.shouldContain("vars")
    }

    it("has resources as the only resources directory") {
      val main = project.extensions.getByType(SourceSetContainer::class.java).getByName("main")
      main.resources.srcDirs shouldHaveSize 1
      main.resources.srcDirs.first().name shouldBe "resources"
    }

    it("has no Java sources") {
      val main = project.extensions.getByType(SourceSetContainer::class.java).getByName("main")
      main.java.srcDirs.shouldBeEmpty()
    }
  }

  describe("configurations") {
    it("jenkinsPlugin is a user-facing declaration bucket") {
      val config = project.configurations.getByName("jenkinsPlugin")
      config.isCanBeResolved shouldBe false
      config.isCanBeConsumed shouldBe false
      config.description.shouldNotBeNull().shouldNotBeBlank()
    }

    listOf(
      "jenkinsPluginClasspath",
      "jenkinsPluginHpis",
      "jenkinsPipelineUnit",
      "jenkinsTestHarness",
      "sharedLibraryIvy",
    ).forEach { name ->
      describe(name) {
        it("is not visible") {
          project.configurations.getByName(name).isVisible.shouldBeFalse()
        }
        it("has a description") {
          project.configurations.getByName(name).description.shouldNotBeNull().shouldNotBeBlank()
        }
      }
    }
  }

  describe("tasks") {
    it("integrationTest is in the verification group") {
      val task = project.tasks.getByName("integrationTest")
      task.shouldBeInstanceOf<org.gradle.api.tasks.testing.Test>()
      task.group shouldBe JavaBasePlugin.VERIFICATION_GROUP
      task.description.shouldNotBeBlank()
    }

    it("groovydocJar is created with a description") {
      val task = project.tasks.getByName("groovydocJar")
      task.shouldBeInstanceOf<Jar>()
      task.description.shouldNotBeBlank()
    }

    it("sourcesJar is created with a description") {
      val task = project.tasks.getByName("sourcesJar")
      task.shouldBeInstanceOf<Jar>()
      task.description.shouldNotBeBlank()
    }
  }

  xit("Jenkins Global Library plugin implementation and HPI dependencies are added") {}
  xit("resources directory is available for GDSL support in IntelliJ") {}
  xit("integrationTestPipelineResources is available on integrationRuntimeOnly classpath") {}
})
