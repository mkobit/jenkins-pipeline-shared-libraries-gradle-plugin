package com.mkobit.jenkins.pipelines

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
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
        val project = sharedLibraryProject()
        try {
          val result = project.runner(gradleVersion).withArguments("help").build()
          result.task(":help")!!.outcome shouldBe TaskOutcome.SUCCESS
        } finally {
          project.cleanup()
        }
      }
    }

    describe("expected tasks are registered") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        val project = sharedLibraryProject()
        try {
          val result = project.runner(gradleVersion).withArguments("tasks", "--all").build()
          result.output shouldContain "integrationTest"
          result.output shouldContain "groovydocJar"
          result.output shouldContain "sourcesJar"
        } finally {
          project.cleanup()
        }
      }
    }

    describe("check lifecycle includes integrationTest") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        val project = sharedLibraryProject()
        try {
          val result = project.runner(gradleVersion).withArguments("check", "--dry-run").build()
          result.output shouldContain ":integrationTest"
        } finally {
          project.cleanup()
        }
      }
    }

    describe("jenkinsPlugin configuration accepts dependencies") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        val project =
          TestProjectBuilder().apply {
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
          }
        try {
          val result = project.runner(gradleVersion).withArguments("help").build()
          result.task(":help")!!.outcome shouldBe TaskOutcome.SUCCESS
        } finally {
          project.cleanup()
        }
      }
    }
  })
