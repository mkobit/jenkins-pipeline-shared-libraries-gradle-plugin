package com.mkobit.jenkins.pipelines

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import testsupport.DEFAULT_CORE_VERSION
import testsupport.DEFAULT_PIPELINE_UNIT_VERSION
import testsupport.DEFAULT_TEST_HARNESS_VERSION
import testsupport.TestProjectBuilder
import testsupport.TestedGradleVersion

// Smoke-tier: inspects declared dependency coordinates only — no artifact resolution, no network.
class SharedLibraryExtensionTest :
  DescribeSpec({
    // Prints declared deps on all relevant configurations without resolving artifacts.
    val printDepsTask =
      """
      tasks.register("printDeclaredDeps") {
          val pluginDeps = configurations["jenkinsPlugin"].dependencies
          val testDeps = configurations["testImplementation"].dependencies
          val integrationDeps = configurations["integrationTestImplementation"].dependencies
          doLast {
              pluginDeps.forEach { println("plugin:" + it.group + ":" + it.name + ":" + it.version) }
              testDeps.forEach { println("test:" + it.group + ":" + it.name + ":" + it.version) }
              integrationDeps.forEach { println("integration:" + it.group + ":" + it.name + ":" + it.version) }
          }
      }
      """.trimIndent()

    fun baseProject(extraConfig: String = ""): TestProjectBuilder =
      TestProjectBuilder().apply {
        buildFile.writeText(
          """
          plugins {
              id("com.mkobit.jenkins.pipelines.shared-library")
          }
          $extraConfig
          $printDepsTask
          """.trimIndent(),
        )
      }

    describe("jenkins.version default is the plugin built-in core version") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        baseProject().use { project ->
          val result = project.runner(gradleVersion).withArguments("printDeclaredDeps").build()
          result.output shouldContain "plugin:org.jenkins-ci.main:jenkins-core:${DEFAULT_CORE_VERSION}"
        }
      }
    }

    describe("jenkins.version override changes jenkins-core coordinate") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        baseProject(
          """
          sharedLibrary {
              jenkins {
                  version = "2.123.4"
              }
          }
          """.trimIndent(),
        ).use { project ->
          val result = project.runner(gradleVersion).withArguments("printDeclaredDeps").build()
          result.output shouldContain "plugin:org.jenkins-ci.main:jenkins-core:2.123.4"
          result.output shouldNotContain "plugin:org.jenkins-ci.main:jenkins-core:${DEFAULT_CORE_VERSION}"
        }
      }
    }

    describe("jenkins.testHarnessVersion default is the plugin built-in test harness version") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        baseProject().use { project ->
          val result = project.runner(gradleVersion).withArguments("printDeclaredDeps").build()
          result.output shouldContain
            "integration:org.jenkins-ci.main:jenkins-test-harness:${DEFAULT_TEST_HARNESS_VERSION}"
        }
      }
    }

    describe("pipelineUnitVersion default is the plugin built-in JPU version") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        baseProject().use { project ->
          val result = project.runner(gradleVersion).withArguments("printDeclaredDeps").build()
          result.output shouldContain "test:com.lesfurets:jenkins-pipeline-unit:$DEFAULT_PIPELINE_UNIT_VERSION"
        }
      }
    }

    describe("pipelineUnitVersion override changes JPU coordinate") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        baseProject(
          """
          sharedLibrary {
              pipelineUnitVersion = "9.9.9"
          }
          """.trimIndent(),
        ).use { project ->
          val result = project.runner(gradleVersion).withArguments("printDeclaredDeps").build()
          result.output shouldContain "test:com.lesfurets:jenkins-pipeline-unit:9.9.9"
          result.output shouldNotContain "test:com.lesfurets:jenkins-pipeline-unit:$DEFAULT_PIPELINE_UNIT_VERSION"
        }
      }
    }

    describe("jenkins.testHarnessVersion override changes test harness coordinate") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        baseProject(
          """
          sharedLibrary {
              jenkins {
                  testHarnessVersion = "9999.vFAKE"
              }
          }
          """.trimIndent(),
        ).use { project ->
          val result = project.runner(gradleVersion).withArguments("printDeclaredDeps").build()
          result.output shouldContain "integration:org.jenkins-ci.main:jenkins-test-harness:9999.vFAKE"
          result.output shouldNotContain
            "integration:org.jenkins-ci.main:jenkins-test-harness:${DEFAULT_TEST_HARNESS_VERSION}"
        }
      }
    }
  })
