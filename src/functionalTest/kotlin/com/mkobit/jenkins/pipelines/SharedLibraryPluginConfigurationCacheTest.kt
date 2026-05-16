package com.mkobit.jenkins.pipelines

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome
import testsupport.gradle.TestedGradleVersion
import testsupport.gradle.withTestProject
import kotlin.io.path.writeText

class SharedLibraryPluginConfigurationCacheTest :
  DescribeSpec({
    describe("configuration cache: second run reuses stored entry") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withTestProject {
          buildFile.writeText(
            """
            plugins {
                id("com.mkobit.jenkins.pipelines.shared-library")
            }
            """.trimIndent(),
          )

          val store =
            runner(gradleVersion)
              .withArguments("generateLocalLibraryFiles", "--configuration-cache")
              .build()
          store.task(":generateLocalLibraryFiles").shouldNotBeNull().outcome shouldBe TaskOutcome.SUCCESS
          store.output shouldContain "Configuration cache entry stored"

          val reuse =
            runner(gradleVersion)
              .withArguments("generateLocalLibraryFiles", "--configuration-cache")
              .build()
          reuse.task(":generateLocalLibraryFiles").shouldNotBeNull().outcome shouldBe TaskOutcome.UP_TO_DATE
          reuse.output shouldContain "Reusing configuration cache"
        }
      }
    }
  })
