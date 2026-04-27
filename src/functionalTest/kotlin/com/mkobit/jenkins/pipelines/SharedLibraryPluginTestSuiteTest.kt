package com.mkobit.jenkins.pipelines

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import org.gradle.testkit.runner.TaskOutcome
import testsupport.JENKINS_BOM
import testsupport.TestProjectBuilder
import testsupport.TestedGradleVersion

// Tests that the shared-library plugin correctly wires jenkinsPlugin JARs into
// consumer-defined test suites across all three JVM languages.
// Requires the Jenkins Maven repo — exclude with -P kotest.tags=!resolution.
@Tags("resolution")
class SharedLibraryPluginTestSuiteTest :
  DescribeSpec({
    fun baseProject(): TestProjectBuilder =
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
          rootProject.name = "suite-test"
          """.trimIndent(),
        )
      }

    describe("Java-only test suite: Jenkins API types compile without Groovy dependency") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        baseProject()
          .apply {
            buildFile.writeText(
              """
              plugins {
                  id("com.mkobit.jenkins.pipelines.shared-library")
                  java
              }
              dependencies {
                  jenkinsPlugin(platform("$JENKINS_BOM"))
              }
              testing {
                  suites {
                      val test by getting(JvmTestSuite::class) {
                          useJUnitJupiter()
                      }
                  }
              }
              """.trimIndent(),
            )
            file("test/unit/java/com/example/JenkinsApiTest.java").writeText(
              """
              package com.example;
              import hudson.model.Item;
              import org.junit.jupiter.api.Test;
              class JenkinsApiTest {
                  @Test
                  void jenkinsApiOnClasspath() {
                      // Compilation succeeds when hudson.model.Item is resolvable
                      Class<?> c = Item.class;
                  }
              }
              """.trimIndent(),
            )
          }.use { project ->
            val result =
              project
                .runner(gradleVersion)
                .withArguments("compileTestJava")
                .build()
            result.task(":compileTestJava")!!.outcome shouldBe TaskOutcome.SUCCESS
          }
      }
    }

    describe("Groovy test suite: Spock test compiles and groovy-all is excluded") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        baseProject()
          .apply {
            buildFile.writeText(
              """
              plugins {
                  id("com.mkobit.jenkins.pipelines.shared-library")
              }
              dependencies {
                  jenkinsPlugin(platform("$JENKINS_BOM"))
                  testImplementation("org.spockframework:spock-core:2.3-groovy-3.0")
              }
              """.trimIndent(),
            )
            file("test/unit/groovy/com/example/JenkinsSpockTest.groovy").writeText(
              """
              package com.example
              import hudson.model.Item
              import spock.lang.Specification
              class JenkinsSpockTest extends Specification {
                  def "Jenkins API types are available"() {
                      expect: Item.class != null
                  }
              }
              """.trimIndent(),
            )
          }.use { project ->
            val depsResult =
              project
                .runner(gradleVersion)
                .withArguments("dependencies", "--configuration", "testRuntimeClasspath")
                .build()
            depsResult.output shouldNotContain "groovy-all"

            val compileResult =
              project
                .runner(gradleVersion)
                .withArguments("compileTestGroovy")
                .build()
            compileResult.task(":compileTestGroovy")!!.outcome shouldBe TaskOutcome.SUCCESS
          }
      }
    }

    describe("Kotlin test suite: Kotest test compiles with Jenkins API on classpath") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        baseProject()
          .apply {
            buildFile.writeText(
              """
              plugins {
                  id("com.mkobit.jenkins.pipelines.shared-library")
                  kotlin("jvm") version "2.0.21"
              }
              dependencies {
                  jenkinsPlugin(platform("$JENKINS_BOM"))
                  testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
                  testImplementation("io.kotest:kotest-assertions-core:5.9.1")
              }
              """.trimIndent(),
            )
            file("src/test/kotlin/com/example/JenkinsKotestTest.kt").writeText(
              """
              package com.example
              import hudson.model.Item
              import io.kotest.core.spec.style.StringSpec
              import io.kotest.matchers.shouldNotBe
              class JenkinsKotestTest : StringSpec({
                  "Jenkins API types are available" {
                      Item::class.java shouldNotBe null
                  }
              })
              """.trimIndent(),
            )
          }.use { project ->
            val result =
              project
                .runner(gradleVersion)
                .withArguments("compileTestKotlin")
                .build()
            result.task(":compileTestKotlin")!!.outcome shouldBe TaskOutcome.SUCCESS
          }
      }
    }
  })
