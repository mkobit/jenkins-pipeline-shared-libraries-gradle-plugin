package com.mkobit.jenkins.pipelines

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.string.shouldContain
import testsupport.gradle.TestedGradleVersion
import testsupport.gradle.withTestProject
import testsupport.jenkins.jenkinsSettings
import kotlin.io.path.writeText

/** Tests for the `sharedLibrarySourceElements` outgoing variant. */
class SharedLibraryPluginVariantTest :
  DescribeSpec({
    describe("sharedLibrarySourceElements") {
      describe("outgoingVariants task reports the variant with Category=shared-library-source") {
        withData(TestedGradleVersion.filtered) { gradleVersion ->
          withTestProject {
            settingsFile.writeText(jenkinsSettings("variant-test"))
            buildFile.writeText(
              """
              plugins {
                  id("com.mkobit.jenkins.pipelines.shared-library")
              }
              """.trimIndent(),
            )

            val result =
              runner(gradleVersion)
                .withArguments("outgoingVariants")
                .build()

            result.output shouldContain "sharedLibrarySourceElements"
            result.output shouldContain "org.gradle.category = shared-library-source"
          }
        }
      }

      describe("artifact path is build/sharedLibrarySource/{libraryName}/ after sync") {
        withData(TestedGradleVersion.filtered) { gradleVersion ->
          withTestProject {
            settingsFile.writeText(jenkinsSettings("variant-test"))
            buildFile.writeText(
              """
              plugins {
                  id("com.mkobit.jenkins.pipelines.shared-library")
              }
              tasks.register("printVariantArtifacts") {
                  val elements = configurations.getByName("sharedLibrarySourceElements")
                  doLast {
                      elements.outgoing.artifacts.forEach {
                          println("artifact=" + it.file.absolutePath)
                      }
                  }
              }
              """.trimIndent(),
            )
            file("vars/myStep.groovy").writeText("def call() {}")

            val result =
              runner(gradleVersion)
                .withArguments("printVariantArtifacts")
                .build()

            // Artifact path: build/sharedLibrarySource/{libraryName}/
            // libraryName defaults to project.name which is set to "variant-test" via jenkinsSettings
            result.output shouldContain "sharedLibrarySource"
            result.output shouldContain "variant-test"
          }
        }
      }
    }
  })
