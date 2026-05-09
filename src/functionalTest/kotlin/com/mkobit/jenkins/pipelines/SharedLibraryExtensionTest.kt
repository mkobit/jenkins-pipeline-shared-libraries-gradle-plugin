package com.mkobit.jenkins.pipelines

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import testsupport.DEFAULT_CORE_VERSION
import testsupport.DEFAULT_PIPELINE_UNIT_VERSION
import testsupport.DEFAULT_TEST_HARNESS_VERSION
import testsupport.TestProject
import testsupport.TestedGradleVersion
import testsupport.withTestProject
import kotlin.io.path.writeText

// Smoke-tier: inspects declared dependency coordinates only — no artifact resolution, no network.
class SharedLibraryExtensionTest :
  DescribeSpec({
    // Prints declared deps on all relevant configurations without resolving artifacts.
    val printDepsTask =
      """
      tasks.register("printDeclaredDeps") {
          doLast {
              configurations.getByName("jenkinsPlugin").dependencies.forEach { println("plugin:" + it.group + ":" + it.name + ":" + it.version) }
              configurations.getByName("testImplementation").dependencies.forEach { println("test:" + it.group + ":" + it.name + ":" + it.version) }
              configurations.getByName("integrationTestImplementation").dependencies.forEach { println("integration:" + it.group + ":" + it.name + ":" + it.version) }
          }
      }
      """.trimIndent()

    fun withBaseProject(
      extraConfig: String = "",
      block: TestProject.() -> Unit,
    ) = withTestProject {
      buildFile.writeText(
        """
        plugins {
            id("com.mkobit.jenkins.pipelines.shared-library")
        }
        $extraConfig
        $printDepsTask
        """.trimIndent(),
      )
      block()
    }

    describe("jenkins.version default is the plugin built-in core version") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withBaseProject {
          val result = runner(gradleVersion).withArguments("printDeclaredDeps").build()
          result.output shouldContain "plugin:org.jenkins-ci.main:jenkins-core:${DEFAULT_CORE_VERSION}"
        }
      }
    }

    describe("jenkins.version override changes jenkins-core coordinate") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withBaseProject(
          """
          sharedLibrary {
              jenkins {
                  version = "2.123.4"
              }
          }
          """.trimIndent(),
        ) {
          val result = runner(gradleVersion).withArguments("printDeclaredDeps").build()
          result.output shouldContain "plugin:org.jenkins-ci.main:jenkins-core:2.123.4"
          result.output shouldNotContain "plugin:org.jenkins-ci.main:jenkins-core:${DEFAULT_CORE_VERSION}"
        }
      }
    }

    describe("jenkins.testHarnessVersion default is the plugin built-in test harness version") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withBaseProject {
          val result = runner(gradleVersion).withArguments("printDeclaredDeps").build()
          result.output shouldContain
            "integration:org.jenkins-ci.main:jenkins-test-harness:${DEFAULT_TEST_HARNESS_VERSION}"
        }
      }
    }

    describe("pipelineUnitVersion default is the plugin built-in JPU version") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withBaseProject {
          val result = runner(gradleVersion).withArguments("printDeclaredDeps").build()
          result.output shouldContain "test:com.lesfurets:jenkins-pipeline-unit:$DEFAULT_PIPELINE_UNIT_VERSION"
        }
      }
    }

    describe("pipelineUnitVersion override changes JPU coordinate") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withBaseProject(
          """
          sharedLibrary {
              pipelineUnitVersion = "9.9.9"
          }
          """.trimIndent(),
        ) {
          val result = runner(gradleVersion).withArguments("printDeclaredDeps").build()
          result.output shouldContain "test:com.lesfurets:jenkins-pipeline-unit:9.9.9"
          result.output shouldNotContain "test:com.lesfurets:jenkins-pipeline-unit:$DEFAULT_PIPELINE_UNIT_VERSION"
        }
      }
    }

    describe("jenkins.testHarnessVersion override changes test harness coordinate") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withBaseProject(
          """
          sharedLibrary {
              jenkins {
                  testHarnessVersion = "9999.vFAKE"
              }
          }
          """.trimIndent(),
        ) {
          val result = runner(gradleVersion).withArguments("printDeclaredDeps").build()
          result.output shouldContain "integration:org.jenkins-ci.main:jenkins-test-harness:9999.vFAKE"
          result.output shouldNotContain
            "integration:org.jenkins-ci.main:jenkins-test-harness:${DEFAULT_TEST_HARNESS_VERSION}"
        }
      }
    }
  })
