package com.mkobit.jenkins.pipelines

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import org.gradle.testkit.runner.TaskOutcome
import testsupport.TestProject
import testsupport.TestedGradleVersion
import testsupport.withTestProject
import kotlin.io.path.writeText

/**
 * Tests that the shared-library plugin correctly wires jenkinsPlugin JARs into
 * consumer-defined test suites across all three JVM languages.
 * Requires the Jenkins Maven repo — exclude with `-P kotest.tags=!resolution`.
 */
@Tags("resolution")
class SharedLibraryPluginTestSuiteTest :
  DescribeSpec({
    val settingsContent =
      """
      dependencyResolutionManagement {
          repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
          repositories {
              mavenCentral()
              maven("https://repo.jenkins-ci.org/public/")
          }
      }
      rootProject.name = "suite-test"
      """.trimIndent()

    fun withBaseProject(block: TestProject.() -> Unit) =
      withTestProject {
        settingsFile.writeText(settingsContent)
        block()
      }

    describe("Java-only test suite: Jenkins API types compile without Groovy dependency") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withBaseProject {
          buildFile.writeText(
            """
            plugins {
                id("com.mkobit.jenkins.pipelines.shared-library")
                java
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
          val result = runner(gradleVersion).withArguments("compileTestJava").build()
          result.task(":compileTestJava")!!.outcome shouldBe TaskOutcome.SUCCESS
        }
      }
    }

    describe("Groovy test suite: Spock test compiles and groovy-all is excluded") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withBaseProject {
          buildFile.writeText(
            """
            plugins {
                id("com.mkobit.jenkins.pipelines.shared-library")
            }
            dependencies {
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
          val depsResult =
            runner(gradleVersion)
              .withArguments("dependencies", "--configuration", "testRuntimeClasspath")
              .build()
          depsResult.output shouldNotContain "groovy-all"

          val compileResult = runner(gradleVersion).withArguments("compileTestGroovy").build()
          compileResult.task(":compileTestGroovy")!!.outcome shouldBe TaskOutcome.SUCCESS
        }
      }
    }

    describe("Kotlin test suite: Kotest test compiles with Jenkins API on classpath") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withBaseProject {
          buildFile.writeText(
            """
            plugins {
                id("com.mkobit.jenkins.pipelines.shared-library")
                kotlin("jvm") version "2.0.21"
            }
            dependencies {
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
          val result = runner(gradleVersion).withArguments("compileTestKotlin").build()
          result.task(":compileTestKotlin")!!.outcome shouldBe TaskOutcome.SUCCESS
        }
      }
    }

    describe("consumer-registered JUnit Jupiter integrationTest suite compiles") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withBaseProject {
          buildFile.writeText(
            """
            plugins {
                id("com.mkobit.jenkins.pipelines.shared-library")
                java
            }
            testing {
                suites {
                    named<JvmTestSuite>("integrationTest") {
                        useJUnitJupiter()
                    }
                }
            }
            """.trimIndent(),
          )
          file("test/integration/java/com/example/JUnit6RuleTest.java").writeText(
            """
            package com.example;
            import hudson.model.Item;
            import org.junit.jupiter.api.Test;
            class JUnit6RuleTest {
                @Test
                void jenkinsApiOnClasspath() {
                    assert Item.class != null;
                }
            }
            """.trimIndent(),
          )
          val result = runner(gradleVersion).withArguments("compileIntegrationTestJava").build()
          result.task(":compileIntegrationTestJava")!!.outcome shouldBe TaskOutcome.SUCCESS
        }
      }
    }

    describe("useJenkinsTestRunnerSuite opt-in wires jenkins-test-harness onto the suite") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withBaseProject {
          buildFile.writeText(
            """
            plugins {
                id("com.mkobit.jenkins.pipelines.shared-library")
                java
            }
            testing {
                suites {
                    register<JvmTestSuite>("integrationTestJunit6") {
                        sharedLibrary.useJenkinsTestRunnerSuite(this)
                        useJUnitJupiter()
                        sources {
                            java.setSrcDirs(listOf("test/integration-junit6/java"))
                        }
                    }
                }
            }
            tasks.check { dependsOn("integrationTestJunit6") }
            """.trimIndent(),
          )
          file("test/integration-junit6/java/com/example/ExtraJUnit6Test.java").writeText(
            """
            package com.example;
            import org.jvnet.hudson.test.JenkinsRule;
            import org.junit.jupiter.api.Test;
            class ExtraJUnit6Test {
                @Test
                void jenkinsTestHarnessTypesAvailable() {
                    Class<?> c = JenkinsRule.class;
                }
            }
            """.trimIndent(),
          )
          val result = runner(gradleVersion).withArguments("compileIntegrationTestJunit6Java").build()
          result.task(":compileIntegrationTestJunit6Java")!!.outcome shouldBe TaskOutcome.SUCCESS
        }
      }
    }
  })
