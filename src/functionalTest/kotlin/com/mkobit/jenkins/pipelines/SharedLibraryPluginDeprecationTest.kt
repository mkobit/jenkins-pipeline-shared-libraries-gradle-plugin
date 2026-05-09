package com.mkobit.jenkins.pipelines

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.string.shouldNotContain
import testsupport.TestedGradleVersion
import testsupport.withTestProject
import kotlin.io.path.writeText

class SharedLibraryPluginDeprecationTest :
  DescribeSpec({
    describe("no deprecation warnings emitted on plugin application") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withTestProject {
          buildFile.writeText(
            """
            plugins {
                id("com.mkobit.jenkins.pipelines.shared-library")
            }
            """.trimIndent(),
          )
          val result =
            runner(gradleVersion)
              .withArguments("help", "--warning-mode=fail")
              .build()
          result.output shouldNotContain "Deprecated Gradle features were used"
        }
      }
    }
  })
