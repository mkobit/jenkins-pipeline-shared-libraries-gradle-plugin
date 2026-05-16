package com.mkobit.jenkins.pipelines

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.gradle.testkit.runner.TaskOutcome
import testsupport.gradle.TestedGradleVersion
import testsupport.gradle.withTestProject
import kotlin.io.path.writeText

class SharedLibraryPluginCacheableTaskTest :
  DescribeSpec({
    val sharedLibraryPluginBuild =
      """
      plugins {
          id("com.mkobit.jenkins.pipelines.shared-library")
      }
      """.trimIndent()

    // Routes the local build cache into the project directory so all GradleRunner instances
    // within a single test share the same cache (the default cache lives in the TestKit
    // Gradle user home, which is isolated per GradleRunner instance when GRADLE_USER_HOME
    // is not set in the environment).
    val buildCacheSettings =
      """
      buildCache {
          local {
              directory = rootDir.resolve("build/.gradle/build-cache")
          }
      }
      """.trimIndent()

    describe("generateLocalLibraryFiles") {
      describe("is UP-TO-DATE on second run when inputs are unchanged") {
        withData(TestedGradleVersion.filtered) { gradleVersion ->
          withTestProject {
            buildFile.writeText(sharedLibraryPluginBuild)

            runner(gradleVersion).withArguments("generateLocalLibraryFiles").build()
              .task(":generateLocalLibraryFiles").shouldNotBeNull().outcome shouldBe TaskOutcome.SUCCESS

            runner(gradleVersion).withArguments("generateLocalLibraryFiles").build()
              .task(":generateLocalLibraryFiles").shouldNotBeNull().outcome shouldBe TaskOutcome.UP_TO_DATE
          }
        }
      }

      describe("re-runs when autoRegisterLibrary input changes") {
        withData(TestedGradleVersion.filtered) { gradleVersion ->
          withTestProject {
            buildFile.writeText(sharedLibraryPluginBuild)

            runner(gradleVersion).withArguments("generateLocalLibraryFiles").build()
              .task(":generateLocalLibraryFiles").shouldNotBeNull().outcome shouldBe TaskOutcome.SUCCESS

            buildFile.writeText(
              """
              plugins {
                  id("com.mkobit.jenkins.pipelines.shared-library")
              }
              sharedLibrary {
                  autoRegisterLibrary = false
              }
              """.trimIndent(),
            )

            runner(gradleVersion).withArguments("generateLocalLibraryFiles").build()
              .task(":generateLocalLibraryFiles").shouldNotBeNull().outcome shouldBe TaskOutcome.SUCCESS
          }
        }
      }

      describe("is loaded FROM-CACHE when outputs are deleted and cache is populated") {
        withData(TestedGradleVersion.filtered) { gradleVersion ->
          withTestProject {
            settingsFile.writeText(buildCacheSettings)
            buildFile.writeText(sharedLibraryPluginBuild)

            runner(gradleVersion).withArguments("generateLocalLibraryFiles", "--build-cache").build()
              .task(":generateLocalLibraryFiles").shouldNotBeNull().outcome shouldBe TaskOutcome.SUCCESS

            dir.resolve("build/generated-src/localLibraryRetriever").toFile().deleteRecursively()

            runner(gradleVersion).withArguments("generateLocalLibraryFiles", "--build-cache").build()
              .task(":generateLocalLibraryFiles").shouldNotBeNull().outcome shouldBe TaskOutcome.FROM_CACHE
          }
        }
      }
    }

    describe("extractJenkinsCodeNarcConfig") {
      describe("is UP-TO-DATE on second run") {
        withData(TestedGradleVersion.filtered) { gradleVersion ->
          withTestProject {
            buildFile.writeText(sharedLibraryPluginBuild)

            runner(gradleVersion).withArguments("extractJenkinsCodeNarcConfig").build()
              .task(":extractJenkinsCodeNarcConfig").shouldNotBeNull().outcome shouldBe TaskOutcome.SUCCESS

            runner(gradleVersion).withArguments("extractJenkinsCodeNarcConfig").build()
              .task(":extractJenkinsCodeNarcConfig").shouldNotBeNull().outcome shouldBe TaskOutcome.UP_TO_DATE
          }
        }
      }

      describe("is loaded FROM-CACHE when output is deleted and cache is populated") {
        withData(TestedGradleVersion.filtered) { gradleVersion ->
          withTestProject {
            settingsFile.writeText(buildCacheSettings)
            buildFile.writeText(sharedLibraryPluginBuild)

            runner(gradleVersion).withArguments("extractJenkinsCodeNarcConfig", "--build-cache").build()
              .task(":extractJenkinsCodeNarcConfig").shouldNotBeNull().outcome shouldBe TaskOutcome.SUCCESS

            dir.resolve("build/generated/codenarc/codenarc-jenkins.xml").toFile().delete()

            runner(gradleVersion).withArguments("extractJenkinsCodeNarcConfig", "--build-cache").build()
              .task(":extractJenkinsCodeNarcConfig").shouldNotBeNull().outcome shouldBe TaskOutcome.FROM_CACHE
          }
        }
      }
    }
  })
