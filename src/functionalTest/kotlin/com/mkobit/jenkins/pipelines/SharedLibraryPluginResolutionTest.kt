package com.mkobit.jenkins.pipelines

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.inspectors.forNone
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldNotContain
import testsupport.JENKINS_BOM
import testsupport.TestProjectBuilder
import testsupport.TestedGradleVersion
import testsupport.WORKFLOW_API

// Resolution-tier tests hit the Jenkins Maven repo on first run (cold cache) and are fast
// on subsequent runs once Gradle's module cache is warm.
// Exclude from PR checks with -P kotest.tags=!resolution; run on merge or scheduled builds.
@Tags("resolution")
class SharedLibraryPluginResolutionTest :
  DescribeSpec({
    fun jenkinsProject(): TestProjectBuilder =
      TestProjectBuilder().apply {
        settingsFile.writeText(
          """
          dependencyResolutionManagement {
              repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
              repositories {
                  mavenCentral()
                  maven("https://repo.jenkins-ci.org/public/")
              }
          }
          rootProject.name = "resolution-test"
          """.trimIndent(),
        )
        buildFile.writeText(
          """
          plugins {
              id("com.mkobit.jenkins.pipelines.shared-library")
          }
          dependencies {
              jenkinsPlugin(platform("$JENKINS_BOM"))
              jenkinsPlugin("$WORKFLOW_API")
          }
          tasks.register("printResolvedArtifacts") {
              val testRt = configurations["testRuntimeClasspath"]
              val hpis = configurations["jenkinsPluginHpis"]
              val compileClasspath = configurations["compileClasspath"]
              val runtimeClasspath = configurations["runtimeClasspath"]
              doLast {
                  testRt.resolvedConfiguration.resolvedArtifacts.forEach {
                      println("testRuntime:" + it.file.name)
                  }
                  hpis.incoming.artifactView { isLenient = true }.artifacts.forEach {
                      println("hpis:" + it.file.name)
                  }
                  compileClasspath.resolvedConfiguration.resolvedArtifacts.forEach {
                      println("compile:" + it.file.name)
                  }
                  runtimeClasspath.resolvedConfiguration.resolvedArtifacts.forEach {
                      println("runtime:" + it.file.name)
                  }
              }
          }
          """.trimIndent(),
        )
      }

    describe("testRuntimeClasspath") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        jenkinsProject().use { project ->
          val result =
            project
              .runner(gradleVersion)
              .withArguments("printResolvedArtifacts")
              .build()

          val testRuntimeFiles =
            result.output
              .lines()
              .filter { it.startsWith("testRuntime:") }
              .map { it.removePrefix("testRuntime:") }

          testRuntimeFiles.shouldNotBeEmpty()
          testRuntimeFiles.filter { it.startsWith("workflow-api") }.shouldNotBeEmpty()
          testRuntimeFiles.forNone { it shouldEndWith ".hpi" }
          testRuntimeFiles.forNone { it shouldEndWith ".jpi" }
        }
      }
    }

    describe("jenkinsPluginHpis") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        jenkinsProject().use { project ->
          val result =
            project
              .runner(gradleVersion)
              .withArguments("printResolvedArtifacts")
              .build()

          val hpiFiles =
            result.output
              .lines()
              .filter { it.startsWith("hpis:") }
              .map { it.removePrefix("hpis:") }

          hpiFiles.shouldNotBeEmpty()
          // Jenkins plugin artifacts must appear as .hpi — plain Java lib transitives may appear as .jar via JpiCompatibilityRule.
          hpiFiles.filter { it.startsWith("workflow-api") && it.endsWith(".hpi") }.shouldNotBeEmpty()
        }
      }
    }

    describe("jenkins-core on compile classpath but not main runtime") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        jenkinsProject().use { project ->
          val result =
            project
              .runner(gradleVersion)
              .withArguments("printResolvedArtifacts")
              .build()

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

          compileFiles.filter { it.startsWith("jenkins-core") }.shouldNotBeEmpty()
          runtimeFiles.filter { it.startsWith("jenkins-core") }.shouldBeEmpty()
        }
      }
    }

    describe("groovy-all absent from testRuntimeClasspath and integrationTestRuntimeClasspath") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        TestProjectBuilder()
          .apply {
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
              dependencies {
                  jenkinsPlugin(platform("$JENKINS_BOM"))
                  jenkinsPlugin("$WORKFLOW_API")
              }
              tasks.register("printGroovyAll") {
                  val testRt = configurations["testRuntimeClasspath"]
                  val integrationTestRt = configurations["integrationTestRuntimeClasspath"]
                  doLast {
                      testRt.resolvedConfiguration.resolvedArtifacts.forEach {
                          println("test:" + it.file.name)
                      }
                      integrationTestRt.resolvedConfiguration.resolvedArtifacts.forEach {
                          println("integration:" + it.file.name)
                      }
                  }
              }
              """.trimIndent(),
            )
          }.use { project ->
            val result =
              project
                .runner(gradleVersion)
                .withArguments("printGroovyAll")
                .build()

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
      withData(TestedGradleVersion.entries) { gradleVersion ->
        jenkinsProject().use { project ->
          project.buildFile.appendText(
            """
            |
            |tasks.register("printJenkinsWar") {
            |    val war = configurations["jenkinsWar"]
            |    doLast {
            |        war.resolvedConfiguration.resolvedArtifacts.forEach {
            |            println("war:" + it.file.name)
            |        }
            |    }
            |}
            """.trimMargin(),
          )
          val result =
            project
              .runner(gradleVersion)
              .withArguments("printJenkinsWar")
              .build()

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
      withData(TestedGradleVersion.entries) { gradleVersion ->
        TestProjectBuilder()
          .apply {
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
              dependencies {
                  jenkinsPlugin(platform("$JENKINS_BOM"))
                  jenkinsPlugin("$WORKFLOW_API")
              }
              tasks.register("printCompileClasspath") {
                  val cp = configurations["integrationTestCompileClasspath"]
                  doLast {
                      cp.resolvedConfiguration.resolvedArtifacts.forEach {
                          println("compile:" + it.file.name)
                      }
                  }
              }
              """.trimIndent(),
            )
          }.use { project ->
            val result =
              project
                .runner(gradleVersion)
                .withArguments("printCompileClasspath")
                .build()

            val compileFiles = result.output.lines().filter { it.startsWith("compile:") }
            compileFiles.shouldNotBeEmpty()
            compileFiles.forNone { it shouldContain "groovy-all" }
          }
      }
    }

    describe("integrationTestGroovyAllRuntime contains groovy-all:2.4.x") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        jenkinsProject().use { project ->
          project.buildFile.appendText(
            """
            |
            |tasks.register("printGroovyAllRuntime") {
            |    val rt = configurations["integrationTestGroovyAllRuntime"]
            |    doLast {
            |        rt.resolvedConfiguration.resolvedArtifacts.forEach {
            |            println("groovyAllRuntime:" + it.file.name)
            |        }
            |    }
            |}
            """.trimMargin(),
          )
          val result =
            project
              .runner(gradleVersion)
              .withArguments("printGroovyAllRuntime")
              .build()

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
      withData(TestedGradleVersion.entries) { gradleVersion ->
        // workflow-api is declared without a version — the BOM must supply it.
        // If BOM wiring is broken Gradle throws an unresolvable dependency error,
        // which causes the runner to throw UnexpectedBuildFailure (test fails).
        // The positive assertion is that the resolved line shows a concrete version
        // (not an empty coordinate), proving the BOM actually constrained it.
        jenkinsProject().use { project ->
          val result =
            project
              .runner(gradleVersion)
              .withArguments("dependencies", "--configuration", "testRuntimeClasspath")
              .build()

          val workflowLine =
            result.output.lines().firstOrNull { it.contains("workflow-api") }
          workflowLine shouldContain Regex("workflow-api:\\d")
          result.output shouldNotContain "FAILED"
        }
      }
    }
  })
