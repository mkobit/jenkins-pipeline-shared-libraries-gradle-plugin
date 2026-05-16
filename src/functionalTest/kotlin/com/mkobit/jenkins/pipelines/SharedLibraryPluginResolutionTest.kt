package com.mkobit.jenkins.pipelines

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.inspectors.forAtLeastOne
import io.kotest.inspectors.forNone
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import testsupport.gradle.TestProject
import testsupport.gradle.TestedGradleVersion
import testsupport.gradle.withTestProject
import testsupport.jenkins.WORKFLOW_API
import testsupport.jenkins.jenkinsSettings
import testsupport.kotest.Resolution
import kotlin.io.path.appendText
import kotlin.io.path.writeText

/**
 * Resolution-tier tests hit the Jenkins Maven repo on first run (cold cache) and are fast
 * on subsequent runs once Gradle's module cache is warm.
 * Exclude from PR checks with `-P kotest.tags=!Resolution`; run on merge or scheduled builds.
 */
class SharedLibraryPluginResolutionTest :
  DescribeSpec({
    tags(Resolution)

    val jenkinsProjectBuildFile =
      """
      plugins {
          id("com.mkobit.jenkins.pipelines.shared-library")
      }
      sharedLibrary {
          plugins {
              plugin("$WORKFLOW_API")
          }
      }
      tasks.register("printResolvedArtifacts") {
          doLast {
              configurations.getByName("testRuntimeClasspath").resolvedConfiguration.resolvedArtifacts.forEach {
                  println("testRuntime:" + it.file.name)
              }
              configurations.getByName("jenkinsPluginHpis").incoming.artifactView { isLenient = true }.artifacts.forEach {
                  println("hpis:" + it.file.name)
              }
              configurations.getByName("compileClasspath").resolvedConfiguration.resolvedArtifacts.forEach {
                  println("compile:" + it.file.name)
              }
              configurations.getByName("runtimeClasspath").resolvedConfiguration.resolvedArtifacts.forEach {
                  println("runtime:" + it.file.name)
              }
          }
      }
      """.trimIndent()

    fun withJenkinsProject(block: TestProject.() -> Unit) = withTestProject {
      settingsFile.writeText(jenkinsSettings("resolution-test"))
      buildFile.writeText(jenkinsProjectBuildFile)
      block()
    }

    describe("testRuntimeClasspath") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withJenkinsProject {
          val result = runner(gradleVersion).withArguments("printResolvedArtifacts").build()

          val testRuntimeFiles =
            result.output
              .lines()
              .filter { it.startsWith("testRuntime:") }
              .map { it.removePrefix("testRuntime:") }

          testRuntimeFiles.shouldNotBeEmpty()
          testRuntimeFiles.forAtLeastOne { it shouldStartWith "workflow-api" }
          testRuntimeFiles.forNone { it shouldEndWith ".hpi" }
          testRuntimeFiles.forNone { it shouldEndWith ".jpi" }
        }
      }
    }

    describe("jenkinsPluginHpis") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withJenkinsProject {
          val result = runner(gradleVersion).withArguments("printResolvedArtifacts").build()

          val hpiFiles =
            result.output
              .lines()
              .filter { it.startsWith("hpis:") }
              .map { it.removePrefix("hpis:") }

          hpiFiles.shouldNotBeEmpty()
          // Jenkins plugin artifacts must appear as .hpi — plain Java lib transitives may appear as .jar via JpiCompatibilityRule.
          hpiFiles.forAtLeastOne {
            it shouldStartWith "workflow-api"
            it shouldEndWith ".hpi"
          }
        }
      }
    }

    describe("jenkins-core on compile classpath but not main runtime") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withJenkinsProject {
          val result = runner(gradleVersion).withArguments("printResolvedArtifacts").build()

          val compileFiles =
            result.output
              .lines()
              .filter { it.startsWith("compile:") }
              .map { it.removePrefix("compile:") }

          val runtimeFiles =
            result.output
              .lines()
              .filter { it.startsWith("runtime:") }
              .map { it.removePrefix("runtime:") }

          compileFiles.forAtLeastOne { it shouldStartWith "jenkins-core" }
          runtimeFiles.forNone { it shouldStartWith "jenkins-core" }
        }
      }
    }

    describe("groovy-all absent from testRuntimeClasspath and integrationTestRuntimeClasspath") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withTestProject {
          settingsFile.writeText(
            """
            dependencyResolutionManagement {
                repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                repositories {
                    mavenCentral()
                    maven("https://repo.jenkins-ci.org/public/")
                }
            }
            rootProject.name = "groovy-all-exclusion-test"
            """.trimIndent(),
          )
          buildFile.writeText(
            """
            plugins {
                id("com.mkobit.jenkins.pipelines.shared-library")
            }
            sharedLibrary {
                plugins {
                    plugin("$WORKFLOW_API")
                }
            }
            tasks.register("printGroovyAll") {
                doLast {
                    configurations.getByName("testRuntimeClasspath").resolvedConfiguration.resolvedArtifacts.forEach {
                        println("test:" + it.file.name)
                    }
                    configurations.getByName("integrationTestRuntimeClasspath").resolvedConfiguration.resolvedArtifacts.forEach {
                        println("integration:" + it.file.name)
                    }
                }
            }
            """.trimIndent(),
          )
          val result = runner(gradleVersion).withArguments("printGroovyAll").build()

          val testFiles = result.output.lines().filter { it.startsWith("test:") }
          val integrationFiles = result.output.lines().filter { it.startsWith("integration:") }

          testFiles.shouldNotBeEmpty()
          integrationFiles.shouldNotBeEmpty()
          testFiles.forNone { it shouldContain "groovy-all" }
          integrationFiles.forNone { it shouldContain "groovy-all" }
        }
      }
    }

    describe("jenkinsWar resolves exactly one WAR artifact") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withJenkinsProject {
          buildFile.appendText(
            """

            tasks.register("printJenkinsWar") {
                doLast {
                    configurations.getByName("jenkinsWar").resolvedConfiguration.resolvedArtifacts.forEach {
                        println("war:" + it.file.name)
                    }
                }
            }
            """.trimIndent(),
          )
          val result = runner(gradleVersion).withArguments("printJenkinsWar").build()

          val warFiles =
            result.output
              .lines()
              .filter { it.startsWith("war:") }
              .map { it.removePrefix("war:") }
              .filter { it.endsWith(".war") }

          warFiles.size shouldBe 1
          warFiles.single() shouldContain "jenkins-war"
        }
      }
    }

    describe("groovy-all absent from integrationTestCompileClasspath") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withTestProject {
          settingsFile.writeText(
            """
            dependencyResolutionManagement {
                repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                repositories {
                    mavenCentral()
                    maven("https://repo.jenkins-ci.org/public/")
                }
            }
            rootProject.name = "groovy-all-compile-exclusion-test"
            """.trimIndent(),
          )
          buildFile.writeText(
            """
            plugins {
                id("com.mkobit.jenkins.pipelines.shared-library")
            }
            sharedLibrary {
                plugins {
                    plugin("$WORKFLOW_API")
                }
            }
            tasks.register("printCompileClasspath") {
                doLast {
                    configurations.getByName("integrationTestCompileClasspath").resolvedConfiguration.resolvedArtifacts.forEach {
                        println("compile:" + it.file.name)
                    }
                    configurations.getByName("integrationTestRuntimeClasspath").resolvedConfiguration.resolvedArtifacts.forEach {
                        println("runtime:" + it.file.name)
                    }
                }
            }
            """.trimIndent(),
          )
          val result = runner(gradleVersion).withArguments("printCompileClasspath").build()

          val compileFiles = result.output.lines().filter { it.startsWith("compile:") }
          val runtimeFiles = result.output.lines().filter { it.startsWith("runtime:") }
          compileFiles.shouldNotBeEmpty()
          runtimeFiles.shouldNotBeEmpty()
          compileFiles.forNone { it shouldContain "groovy-all" }
          // ComponentMetadataRule restores jakarta.servlet-api to all variants of jenkins-test-harness.
          // Must be present on both compile (Groovy type-checker) and runtime (JVM class verification
          // before Winstone starts) classpaths.
          compileFiles.forAtLeastOne { it shouldContain "jakarta.servlet-api" }
          runtimeFiles.forAtLeastOne { it shouldContain "jakarta.servlet-api" }
        }
      }
    }

    describe("integrationTestGroovyAllRuntime contains groovy-all:2.4.x") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withJenkinsProject {
          buildFile.appendText(
            """

            tasks.register("printGroovyAllRuntime") {
                doLast {
                    configurations.getByName("integrationTestGroovyAllRuntime").resolvedConfiguration.resolvedArtifacts.forEach {
                        println("groovyAllRuntime:" + it.file.name)
                    }
                }
            }
            """.trimIndent(),
          )
          val result = runner(gradleVersion).withArguments("printGroovyAllRuntime").build()

          val groovyAllFiles =
            result.output
              .lines()
              .filter { it.startsWith("groovyAllRuntime:") }
              .map { it.removePrefix("groovyAllRuntime:") }

          groovyAllFiles.size shouldBe 1
          groovyAllFiles.single() shouldContain "groovy-all"
          groovyAllFiles.single() shouldContain "2.4"
        }
      }
    }

    describe("BOM version constraint propagates through jenkinsPlugin") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        // workflow-api is declared without a version — the BOM must supply it.
        // If BOM wiring is broken Gradle throws an unresolvable dependency error,
        // which causes the runner to throw UnexpectedBuildFailure (test fails).
        // The positive assertion is that the resolved line shows a concrete version
        // (not an empty coordinate), proving the BOM actually constrained it.
        withJenkinsProject {
          val result =
            runner(gradleVersion)
              .withArguments("dependencies", "--configuration", "testRuntimeClasspath")
              .build()

          val workflowLine = result.output.lines().firstOrNull { it.contains("workflow-api") }
          // BOM-constrained versionless deps show "workflow-api -> 1373.x" not "workflow-api:1373"
          workflowLine shouldContain Regex("workflow-api(?::\\d| -> \\d)")
          result.output shouldNotContain "FAILED"
        }
      }
    }
  })
