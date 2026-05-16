package com.mkobit.jenkins.pipelines

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.inspectors.filterMatching
import io.kotest.inspectors.forAtLeastOne
import io.kotest.inspectors.forNone
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import org.gradle.util.GradleVersion
import testsupport.gradle.TestProject
import testsupport.gradle.TestedGradleVersion
import testsupport.gradle.withTestProject
import testsupport.jenkins.JenkinsCompatEntry
import testsupport.jenkins.TestedJenkinsVersion
import testsupport.jenkins.WORKFLOW_API
import testsupport.jenkins.jenkinsSettings
import testsupport.kotest.JenkinsCompat
import kotlin.io.path.writeText

/**
 * Verifies that the plugin correctly wires Jenkins LTS dependency coordinates for each supported
 * LTS version. Run per-version by the jenkins-compat CI job; each run pins `test.jenkins.version`
 * so [TestedJenkinsVersion.filtered] returns a single entry.
 */
class SharedLibraryPluginJenkinsCompatTest :
  DescribeSpec({
    tags(JenkinsCompat)

    val gradleVersion = TestedGradleVersion(GradleVersion.current().version)

    fun buildContent(e: JenkinsCompatEntry) =
      """
      plugins {
          id("com.mkobit.jenkins.pipelines.shared-library")
      }
      sharedLibrary {
          jenkins {
              version = "${e.jenkinsVersion}"
              bomVersion = "${e.jenkinsBomVersion}"
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

    fun withJenkinsCompatProject(
      e: JenkinsCompatEntry,
      block: TestProject.() -> Unit,
    ) = withTestProject {
      settingsFile.writeText(jenkinsSettings("jenkins-compat-test"))
      buildFile.writeText(buildContent(e))
      block()
    }

    withData(TestedJenkinsVersion.filtered) { jenkins ->
      val e = jenkins.entry

      describe("jenkins-core resolves at declared version") {
        withJenkinsCompatProject(e) {
          val result = runner(gradleVersion).withArguments("printClasspaths").build()

          val compileFiles = result.output.lines()
            .filterMatching { it.shouldStartWith("compile:") }
          compileFiles.shouldNotBeEmpty()
          compileFiles.forAtLeastOne { it shouldContain "jenkins-core-${e.jenkinsVersion}" }
        }
      }

      describe("BOM constraint propagates through jenkinsPlugin") {
        withJenkinsCompatProject(e) {
          val result =
            runner(gradleVersion)
              .withArguments("dependencies", "--configuration", "testRuntimeClasspath")
              .build()

          result.output shouldContain Regex("workflow-api:\\d")
          result.output shouldNotContain "FAILED"
        }
      }

      describe("groovy-all absent from testRuntimeClasspath") {
        withJenkinsCompatProject(e) {
          val result = runner(gradleVersion).withArguments("printClasspaths").build()

          val testRuntimeFiles = result.output.lines()
            .filterMatching { it.shouldStartWith("testRuntime:") }
          testRuntimeFiles.shouldNotBeEmpty()
          testRuntimeFiles.forNone { it shouldContain "groovy-all" }
        }
      }

      describe("jenkinsWar resolves exactly one WAR at declared version") {
        withJenkinsCompatProject(e) {
          val result = runner(gradleVersion).withArguments("printClasspaths").build()

          val warFiles = result.output.lines()
            .filterMatching { it.shouldStartWith("war:") }
          warFiles.size shouldBe 1
          warFiles.single() shouldContain "jenkins-war-${e.jenkinsVersion}"
        }
      }
    }
  })
