package com.mkobit.jenkins.pipelines

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome
import testsupport.TestProjectBuilder
import testsupport.TestedGradleVersion

class SharedLibraryPluginSmokeTest :
  DescribeSpec({
    fun sharedLibraryProject(): TestProjectBuilder =
      TestProjectBuilder().apply {
        buildFile.writeText(
          """
          plugins {
              id("com.mkobit.jenkins.pipelines.shared-library")
          }
          """.trimIndent(),
        )
      }

    describe("plugin application") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        sharedLibraryProject().use { project ->
          val result = project.runner(gradleVersion).withArguments("help").build()
          result.task(":help")!!.outcome shouldBe TaskOutcome.SUCCESS
        }
      }
    }

    describe("expected tasks are registered") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        sharedLibraryProject().use { project ->
          val result = project.runner(gradleVersion).withArguments("tasks", "--all").build()
          result.output shouldContain "integrationTest"
          result.output shouldContain "generateLocalLibraryFiles"
          result.output shouldContain "groovydocJar"
          result.output shouldContain "sourcesJar"
          result.output shouldContain "groovydoc"
          result.output shouldContain "compileGroovy"
        }
      }
    }

    describe("generateLocalLibraryFiles produces LocalLibraryRetriever.java and ClassFilter resource") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        sharedLibraryProject().use { project ->
          val result =
            project
              .runner(gradleVersion)
              .withArguments("generateLocalLibraryFiles")
              .build()
          result.task(":generateLocalLibraryFiles")!!.outcome shouldBe TaskOutcome.SUCCESS
          project.dir
            .resolve("build/generated-src/integrationTest/java/com/mkobit/jenkins/pipelines/testing/LocalLibraryRetriever.java")
            .shouldExist()
          project.dir
            .resolve("build/generated-src/integrationTest/resources/META-INF/hudson.remoting.ClassFilter")
            .shouldExist()
        }
      }
    }

    describe("compileIntegrationTestJava depends on generateLocalLibraryFiles") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        sharedLibraryProject().use { project ->
          val result =
            project
              .runner(gradleVersion)
              .withArguments("compileIntegrationTestJava", "--dry-run")
              .build()
          result.output shouldContain ":generateLocalLibraryFiles"
          result.output shouldContain ":compileIntegrationTestJava"
        }
      }
    }

    describe("check lifecycle includes integrationTest") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        sharedLibraryProject().use { project ->
          val result = project.runner(gradleVersion).withArguments("check", "--dry-run").build()
          result.output shouldContain ":integrationTest"
          result.output shouldContain ":test"
        }
      }
    }

    describe("jenkinsPlugin configuration accepts a dependency declaration") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        TestProjectBuilder()
          .apply {
            buildFile.writeText(
              """
              plugins {
                  id("com.mkobit.jenkins.pipelines.shared-library")
              }
              dependencies {
                  jenkinsPlugin("org.example:fake:1.0")
              }
              """.trimIndent(),
            )
          }.use { project ->
            val result = project.runner(gradleVersion).withArguments("help").build()
            result.task(":help")!!.outcome shouldBe TaskOutcome.SUCCESS
          }
      }
    }

    describe("jenkinsPlugin configuration accepts a platform BOM") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        TestProjectBuilder()
          .apply {
            buildFile.writeText(
              """
              plugins {
                  id("com.mkobit.jenkins.pipelines.shared-library")
              }
              dependencies {
                  jenkinsPlugin(platform("org.example:fake-bom:1.0"))
              }
              """.trimIndent(),
            )
          }.use { project ->
            val result = project.runner(gradleVersion).withArguments("help").build()
            result.task(":help")!!.outcome shouldBe TaskOutcome.SUCCESS
          }
      }
    }

    describe("monorepo: test.library.root resolves relative to subproject projectDir, not rootDir") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        TestProjectBuilder()
          .apply {
            settingsFile.writeText(
              """
              rootProject.name = "monorepo-root"
              include(":lib")
              """.trimIndent(),
            )
            buildFile.writeText("")
            file("lib/build.gradle.kts").writeText(
              """
              plugins {
                  id("com.mkobit.jenkins.pipelines.shared-library")
              }
              tasks.register("printLibraryRoot") {
                  val t = tasks.named("integrationTest")
                  doLast {
                      val testTask = t.get() as org.gradle.api.tasks.testing.Test
                      println("root=" + testTask.systemProperties["test.library.root"])
                  }
              }
              """.trimIndent(),
            )
          }.use { project ->
            val result =
              project
                .runner(gradleVersion)
                .withArguments(":lib:printLibraryRoot")
                .build()
            val expectedRoot = project.dir.resolve("lib").absolutePath
            result.output shouldContain "root=$expectedRoot"
          }
      }
    }
  })
