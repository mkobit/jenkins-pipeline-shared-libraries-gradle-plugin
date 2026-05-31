package com.mkobit.jenkins.pipelines

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import testsupport.gradle.TestProject
import testsupport.gradle.TestedGradleVersion
import testsupport.gradle.withTestProject
import kotlin.io.path.writeText

/** Smoke-tier: inspects declared dependency coordinates only — no artifact resolution, no network. */
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
      withData(TestedGradleVersion.all) { gradleVersion ->
        withBaseProject {
          val result = runner(gradleVersion).withArguments("printDeclaredDeps").build()
          result.output shouldContain "plugin:org.jenkins-ci.main:jenkins-core:2.479.1"
        }
      }
    }

    describe("jenkins.version override changes jenkins-core coordinate") {
      withData(TestedGradleVersion.all) { gradleVersion ->
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
          result.output shouldNotContain "plugin:org.jenkins-ci.main:jenkins-core:2.479.1"
        }
      }
    }

    describe("jenkins-test-harness is wired with the plugin's internal minimum version") {
      withData(TestedGradleVersion.all) { gradleVersion ->
        withBaseProject {
          val result = runner(gradleVersion).withArguments("printDeclaredDeps").build()
          result.output shouldContain
            "integration:org.jenkins-ci.main:jenkins-test-harness:2565.vd1eb_7c961d1b_"
        }
      }
    }

    describe("pipelineUnitVersion default is the plugin built-in JPU version") {
      withData(TestedGradleVersion.all) { gradleVersion ->
        withBaseProject {
          val result = runner(gradleVersion).withArguments("printDeclaredDeps").build()
          result.output shouldContain "test:com.lesfurets:jenkins-pipeline-unit:1.29"
        }
      }
    }

    describe("pipelineUnitVersion override changes JPU coordinate") {
      withData(TestedGradleVersion.all) { gradleVersion ->
        withBaseProject(
          """
          sharedLibrary {
              pipelineUnitVersion = "9.9.9"
          }
          """.trimIndent(),
        ) {
          val result = runner(gradleVersion).withArguments("printDeclaredDeps").build()
          result.output shouldContain "test:com.lesfurets:jenkins-pipeline-unit:9.9.9"
          result.output shouldNotContain "test:com.lesfurets:jenkins-pipeline-unit:1.29"
        }
      }
    }

    describe("sharedLibrary.plugins.plugin registers a dependency on jenkinsPlugin configuration") {
      withData(TestedGradleVersion.all) { gradleVersion ->
        withBaseProject(
          """
          sharedLibrary {
              plugins {
                  plugin("org.example:fake:1.0")
              }
          }
          """.trimIndent(),
        ) {
          val result = runner(gradleVersion).withArguments("printDeclaredDeps").build()
          result.output shouldContain "plugin:org.example:fake:1.0"
        }
      }
    }
  })
