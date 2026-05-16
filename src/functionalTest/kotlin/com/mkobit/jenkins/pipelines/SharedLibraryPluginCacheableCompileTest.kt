package com.mkobit.jenkins.pipelines

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.gradle.testkit.runner.TaskOutcome
import testsupport.gradle.TestedGradleVersion
import testsupport.gradle.withTestProject
import testsupport.jenkins.jenkinsSettings
import testsupport.kotest.Resolution
import kotlin.io.path.writeText

class SharedLibraryPluginCacheableCompileTest :
  DescribeSpec({
    tags(Resolution)

    val buildContent =
      """
      plugins {
          id("com.mkobit.jenkins.pipelines.shared-library")
      }
      """.trimIndent()

    describe("compileLocalLibraryRetrieverJava") {
      describe("is UP-TO-DATE on second run when inputs are unchanged") {
        withData(TestedGradleVersion.filtered) { gradleVersion ->
          withTestProject {
            settingsFile.writeText(jenkinsSettings("compile-cache-test"))
            buildFile.writeText(buildContent)

            runner(gradleVersion).withArguments("compileLocalLibraryRetrieverJava").build()
              .task(":compileLocalLibraryRetrieverJava").shouldNotBeNull().outcome shouldBe TaskOutcome.SUCCESS

            runner(gradleVersion).withArguments("compileLocalLibraryRetrieverJava").build()
              .task(":compileLocalLibraryRetrieverJava").shouldNotBeNull().outcome shouldBe TaskOutcome.UP_TO_DATE
          }
        }
      }

      describe("is loaded FROM-CACHE when outputs are deleted and cache is populated") {
        withData(TestedGradleVersion.filtered) { gradleVersion ->
          withTestProject {
            settingsFile.writeText(
              """
              buildCache {
                  local {
                      directory = rootDir.resolve("build/.gradle/build-cache")
                  }
              }
              ${jenkinsSettings("compile-cache-test")}
              """.trimIndent(),
            )
            buildFile.writeText(buildContent)

            runner(gradleVersion).withArguments("compileLocalLibraryRetrieverJava", "--build-cache").build()
              .task(":compileLocalLibraryRetrieverJava").shouldNotBeNull().outcome shouldBe TaskOutcome.SUCCESS

            dir.resolve("build/classes/java/localLibraryRetriever").toFile().deleteRecursively()

            runner(gradleVersion).withArguments("compileLocalLibraryRetrieverJava", "--build-cache").build()
              .task(":compileLocalLibraryRetrieverJava").shouldNotBeNull().outcome shouldBe TaskOutcome.FROM_CACHE
          }
        }
      }
    }
  })
