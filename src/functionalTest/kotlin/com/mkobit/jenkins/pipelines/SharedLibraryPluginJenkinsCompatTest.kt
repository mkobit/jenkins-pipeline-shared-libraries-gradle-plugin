package com.mkobit.jenkins.pipelines

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.inspectors.forAtLeastOne
import io.kotest.inspectors.forNone
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.gradle.util.GradleVersion
import testsupport.TestedGradleVersion
import testsupport.TestedJenkinsVersion
import testsupport.WORKFLOW_API
import testsupport.withTestProject
import kotlin.io.path.writeText

// Verifies that the plugin correctly wires Jenkins LTS dependency coordinates for each supported
// LTS version. Run per-version by functionalTestJenkins* tasks in build.gradle.kts; each task
// pins test.jenkins.version so TestedJenkinsVersion.filtered returns a single entry.
@Tags("jenkins-compat")
class SharedLibraryPluginJenkinsCompatTest :
  DescribeSpec({
    val gradleVersion = TestedGradleVersion(GradleVersion.current().version)

    withData(TestedJenkinsVersion.filtered) { jenkins ->
      val e = jenkins.entry

      val settingsContent =
        """
        dependencyResolutionManagement {
            repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
            repositories {
                mavenCentral()
                maven("https://repo.jenkins-ci.org/public/")
            }
        }
        rootProject.name = "jenkins-compat-test"
        """.trimIndent()

      val buildContent =
        """
        plugins {
            id("com.mkobit.jenkins.pipelines.shared-library")
        }
        sharedLibrary {
            jenkins {
                version = "${e.jenkinsVersion}"
                bomVersion = "${e.jenkinsBomVersion}"
                testHarnessVersion = "${e.jenkinsTestHarness}"
            }
        }
        dependencies {
            jenkinsPlugin("$WORKFLOW_API")
        }
        tasks.register("printClasspaths") {
            doLast {
                configurations.getByName("compileClasspath").resolvedConfiguration.resolvedArtifacts.forEach {
                    println("compile:" + it.file.name)
                }
                configurations.getByName("testRuntimeClasspath").resolvedConfiguration.resolvedArtifacts.forEach {
                    println("testRuntime:" + it.file.name)
                }
                configurations.getByName("jenkinsWar").resolvedConfiguration.resolvedArtifacts
                    .filter { it.file.name.endsWith(".war") }
                    .forEach { println("war:" + it.file.name) }
            }
        }
        """.trimIndent()

      describe("jenkins-core resolves at declared version") {
        withTestProject { project ->
          project.settingsFile.writeText(settingsContent)
          project.buildFile.writeText(buildContent)
          val result =
            project
              .runner(gradleVersion)
              .withArguments("printClasspaths")
              .build()

          val compileFiles =
            result.output.lines().filter { it.startsWith("compile:") }.map { it.removePrefix("compile:") }
          compileFiles.shouldNotBeEmpty()
          compileFiles.forAtLeastOne { it shouldContain "jenkins-core-${e.jenkinsVersion}" }
        }
      }

      describe("BOM constraint propagates through jenkinsPlugin") {
        withTestProject { project ->
          project.settingsFile.writeText(settingsContent)
          project.buildFile.writeText(buildContent)
          val result =
            project
              .runner(gradleVersion)
              .withArguments("dependencies", "--configuration", "testRuntimeClasspath")
              .build()

          result.output shouldContain Regex("workflow-api:\\d")
          result.output shouldNotContain "FAILED"
        }
      }

      describe("groovy-all absent from testRuntimeClasspath") {
        withTestProject { project ->
          project.settingsFile.writeText(settingsContent)
          project.buildFile.writeText(buildContent)
          val result =
            project
              .runner(gradleVersion)
              .withArguments("printClasspaths")
              .build()

          val testRuntimeFiles =
            result.output.lines().filter { it.startsWith("testRuntime:") }.map { it.removePrefix("testRuntime:") }
          testRuntimeFiles.shouldNotBeEmpty()
          testRuntimeFiles.forNone { it shouldContain "groovy-all" }
        }
      }

      describe("jenkinsWar resolves exactly one WAR at declared version") {
        withTestProject { project ->
          project.settingsFile.writeText(settingsContent)
          project.buildFile.writeText(buildContent)
          val result =
            project
              .runner(gradleVersion)
              .withArguments("printClasspaths")
              .build()

          val warFiles =
            result.output.lines().filter { it.startsWith("war:") }.map { it.removePrefix("war:") }
          warFiles.size shouldBe 1
          warFiles.single() shouldContain "jenkins-war-${e.jenkinsVersion}"
        }
      }
    }
  })
