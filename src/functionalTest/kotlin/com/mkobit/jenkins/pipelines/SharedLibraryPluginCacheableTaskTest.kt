package com.mkobit.jenkins.pipelines

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.gradle.testkit.runner.TaskOutcome
import testsupport.gradle.TestedGradleVersion
import testsupport.gradle.withTestProject
import testsupport.jenkins.jenkinsSettings
import kotlin.io.path.writeText

class SharedLibraryPluginCacheableTaskTest :
  DescribeSpec({
    val sharedLibraryPluginBuild =
      """
      plugins {
          id("com.mkobit.jenkins.pipelines.shared-library")
      }
      """.trimIndent()

    val stubJunitTest =
      """
      package com.example;
      import org.junit.jupiter.api.Test;
      class StubTest {
          @Test void passes() {}
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

    describe("syncSharedLibrarySource") {
      describe("is UP-TO-DATE on second run when inputs are unchanged") {
        withData(TestedGradleVersion.all) { gradleVersion ->
          withTestProject {
            buildFile.writeText(sharedLibraryPluginBuild)
            file("src/com/example/Lib.groovy").writeText("class Lib {}")

            runner(gradleVersion)
              .withArguments("syncSharedLibrarySource")
              .build()
              .task(":syncSharedLibrarySource") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }

            runner(gradleVersion)
              .withArguments("syncSharedLibrarySource")
              .build()
              .task(":syncSharedLibrarySource") shouldNotBeNull { outcome shouldBe TaskOutcome.UP_TO_DATE }
          }
        }
      }

      describe("re-runs when a source file changes") {
        withData(TestedGradleVersion.all) { gradleVersion ->
          withTestProject {
            buildFile.writeText(sharedLibraryPluginBuild)
            val libFile = file("src/com/example/Lib.groovy")
            libFile.writeText("class Lib {}")

            runner(gradleVersion)
              .withArguments("syncSharedLibrarySource")
              .build()
              .task(":syncSharedLibrarySource") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }

            libFile.writeText("class Lib { def changed() {} }")

            runner(gradleVersion)
              .withArguments("syncSharedLibrarySource")
              .build()
              .task(":syncSharedLibrarySource") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }
          }
        }
      }

      describe("is loaded FROM-CACHE when outputs are deleted and cache is populated") {
        withData(TestedGradleVersion.all) { gradleVersion ->
          withTestProject {
            settingsFile.writeText(buildCacheSettings)
            buildFile.writeText(sharedLibraryPluginBuild)
            file("vars/myStep.groovy").writeText("def call() {}")

            runner(gradleVersion)
              .withArguments("syncSharedLibrarySource", "--build-cache")
              .build()
              .task(":syncSharedLibrarySource") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }

            dir.resolve("build/sharedLibrarySource").toFile().deleteRecursively()

            runner(gradleVersion)
              .withArguments("syncSharedLibrarySource", "--build-cache")
              .build()
              .task(":syncSharedLibrarySource") shouldNotBeNull { outcome shouldBe TaskOutcome.FROM_CACHE }
          }
        }
      }
    }

    describe("generateLocalLibraryFiles") {
      describe("is UP-TO-DATE on second run when inputs are unchanged") {
        withData(TestedGradleVersion.all) { gradleVersion ->
          withTestProject {
            buildFile.writeText(sharedLibraryPluginBuild)

            runner(gradleVersion)
              .withArguments("generateLocalLibraryFiles")
              .build()
              .task(":generateLocalLibraryFiles") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }

            runner(gradleVersion)
              .withArguments("generateLocalLibraryFiles")
              .build()
              .task(":generateLocalLibraryFiles") shouldNotBeNull { outcome shouldBe TaskOutcome.UP_TO_DATE }
          }
        }
      }

      describe("re-runs when autoRegisterLibrary input changes") {
        withData(TestedGradleVersion.all) { gradleVersion ->
          withTestProject {
            buildFile.writeText(sharedLibraryPluginBuild)

            runner(gradleVersion)
              .withArguments("generateLocalLibraryFiles")
              .build()
              .task(":generateLocalLibraryFiles") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }

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

            runner(gradleVersion)
              .withArguments("generateLocalLibraryFiles")
              .build()
              .task(":generateLocalLibraryFiles") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }
          }
        }
      }

      describe("is loaded FROM-CACHE when outputs are deleted and cache is populated") {
        withData(TestedGradleVersion.all) { gradleVersion ->
          withTestProject {
            settingsFile.writeText(buildCacheSettings)
            buildFile.writeText(sharedLibraryPluginBuild)

            runner(gradleVersion)
              .withArguments("generateLocalLibraryFiles", "--build-cache")
              .build()
              .task(":generateLocalLibraryFiles") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }

            dir.resolve("build/generated-src/localLibraryRetriever").toFile().deleteRecursively()

            runner(gradleVersion)
              .withArguments("generateLocalLibraryFiles", "--build-cache")
              .build()
              .task(":generateLocalLibraryFiles") shouldNotBeNull { outcome shouldBe TaskOutcome.FROM_CACHE }
          }
        }
      }
    }

    describe("extractJenkinsCodeNarcConfig") {
      describe("is UP-TO-DATE on second run") {
        withData(TestedGradleVersion.all) { gradleVersion ->
          withTestProject {
            buildFile.writeText(sharedLibraryPluginBuild)

            runner(gradleVersion)
              .withArguments("extractJenkinsCodeNarcConfig")
              .build()
              .task(":extractJenkinsCodeNarcConfig") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }

            runner(gradleVersion)
              .withArguments("extractJenkinsCodeNarcConfig")
              .build()
              .task(":extractJenkinsCodeNarcConfig") shouldNotBeNull { outcome shouldBe TaskOutcome.UP_TO_DATE }
          }
        }
      }
    }

    describe("extractDefaultCodeNarcConfig") {
      describe("is UP-TO-DATE on second run") {
        withData(TestedGradleVersion.all) { gradleVersion ->
          withTestProject {
            buildFile.writeText(sharedLibraryPluginBuild)

            runner(gradleVersion)
              .withArguments("extractDefaultCodeNarcConfig")
              .build()
              .task(":extractDefaultCodeNarcConfig") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }

            runner(gradleVersion)
              .withArguments("extractDefaultCodeNarcConfig")
              .build()
              .task(":extractDefaultCodeNarcConfig") shouldNotBeNull { outcome shouldBe TaskOutcome.UP_TO_DATE }
          }
        }
      }
    }

    describe("test") {
      describe("is UP-TO-DATE on second run when inputs are unchanged") {
        withData(TestedGradleVersion.all) { gradleVersion ->
          withTestProject {
            settingsFile.writeText(jenkinsSettings("cache-test"))
            buildFile.writeText(sharedLibraryPluginBuild)
            file("test/unit/java/com/example/StubTest.java").writeText(stubJunitTest)

            runner(gradleVersion)
              .withArguments("test")
              .build()
              .task(":test") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }

            runner(gradleVersion)
              .withArguments("test")
              .build()
              .task(":test") shouldNotBeNull { outcome shouldBe TaskOutcome.UP_TO_DATE }
          }
        }
      }

      describe("is loaded FROM-CACHE when outputs are deleted and cache is populated") {
        withData(TestedGradleVersion.all) { gradleVersion ->
          withTestProject {
            settingsFile.writeText(
              """
              $buildCacheSettings
              ${jenkinsSettings("cache-test")}
              """.trimIndent(),
            )
            buildFile.writeText(sharedLibraryPluginBuild)
            file("test/unit/java/com/example/StubTest.java").writeText(stubJunitTest)

            runner(gradleVersion)
              .withArguments("test", "--build-cache")
              .build()
              .task(":test") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }

            dir.resolve("build/test-results/test").toFile().deleteRecursively()
            dir.resolve("build/reports/tests/test").toFile().deleteRecursively()

            runner(gradleVersion)
              .withArguments("test", "--build-cache")
              .build()
              .task(":test") shouldNotBeNull { outcome shouldBe TaskOutcome.FROM_CACHE }
          }
        }
      }
    }

    describe("integrationTest") {
      describe("is UP-TO-DATE on second run when inputs are unchanged") {
        withData(TestedGradleVersion.all) { gradleVersion ->
          withTestProject {
            settingsFile.writeText(jenkinsSettings("cache-integration-test"))
            buildFile.writeText(sharedLibraryPluginBuild)
            file("test/integration/java/com/example/StubTest.java").writeText(stubJunitTest)

            runner(gradleVersion)
              .withArguments("integrationTest")
              .build()
              .task(":integrationTest") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }

            runner(gradleVersion)
              .withArguments("integrationTest")
              .build()
              .task(":integrationTest") shouldNotBeNull { outcome shouldBe TaskOutcome.UP_TO_DATE }
          }
        }
      }

      describe("is loaded FROM-CACHE when outputs are deleted and cache is populated") {
        withData(TestedGradleVersion.all) { gradleVersion ->
          withTestProject {
            settingsFile.writeText(
              """
              $buildCacheSettings
              ${jenkinsSettings("cache-integration-test")}
              """.trimIndent(),
            )
            buildFile.writeText(sharedLibraryPluginBuild)
            file("test/integration/java/com/example/StubTest.java").writeText(stubJunitTest)

            runner(gradleVersion)
              .withArguments("integrationTest", "--build-cache")
              .build()
              .task(":integrationTest") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }

            dir.resolve("build/test-results/integrationTest").toFile().deleteRecursively()
            dir.resolve("build/reports/tests/integrationTest").toFile().deleteRecursively()

            runner(gradleVersion)
              .withArguments("integrationTest", "--build-cache")
              .build()
              .task(":integrationTest") shouldNotBeNull { outcome shouldBe TaskOutcome.FROM_CACHE }
          }
        }
      }
    }

    describe("compileLocalLibraryRetrieverJava") {
      describe("is UP-TO-DATE on second run when inputs are unchanged") {
        withData(TestedGradleVersion.all) { gradleVersion ->
          withTestProject {
            settingsFile.writeText(jenkinsSettings("compile-cache-test"))
            buildFile.writeText(sharedLibraryPluginBuild)

            runner(gradleVersion)
              .withArguments("compileLocalLibraryRetrieverJava")
              .build()
              .task(":compileLocalLibraryRetrieverJava") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }

            runner(gradleVersion)
              .withArguments("compileLocalLibraryRetrieverJava")
              .build()
              .task(":compileLocalLibraryRetrieverJava") shouldNotBeNull { outcome shouldBe TaskOutcome.UP_TO_DATE }
          }
        }
      }

      describe("is loaded FROM-CACHE when outputs are deleted and cache is populated") {
        withData(TestedGradleVersion.all) { gradleVersion ->
          withTestProject {
            settingsFile.writeText(
              """
              $buildCacheSettings
              ${jenkinsSettings("compile-cache-test")}
              """.trimIndent(),
            )
            buildFile.writeText(sharedLibraryPluginBuild)

            runner(gradleVersion)
              .withArguments("compileLocalLibraryRetrieverJava", "--build-cache")
              .build()
              .task(":compileLocalLibraryRetrieverJava") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }

            dir.resolve("build/classes/java/localLibraryRetriever").toFile().deleteRecursively()

            runner(gradleVersion)
              .withArguments("compileLocalLibraryRetrieverJava", "--build-cache")
              .build()
              .task(":compileLocalLibraryRetrieverJava") shouldNotBeNull { outcome shouldBe TaskOutcome.FROM_CACHE }
          }
        }
      }
    }
  })
