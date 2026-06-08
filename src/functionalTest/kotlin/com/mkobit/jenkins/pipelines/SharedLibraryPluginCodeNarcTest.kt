package com.mkobit.jenkins.pipelines

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.paths.shouldBeReadable
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome
import testsupport.gradle.TestProject
import testsupport.gradle.TestedGradleVersion
import testsupport.gradle.withTestProject
import testsupport.jenkins.jenkinsSettings
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Verifies that shared-library + codenarc together fire the Jenkins Enhanced Classpath Rules
 * and catch violations from rulesets/jenkins.xml using the bundled codenarc-jenkins.xml resource.
 */
class SharedLibraryPluginCodeNarcTest :
  DescribeSpec({
    val buildFileContent =
      """
      plugins {
          id("com.mkobit.jenkins.pipelines.shared-library")
          codenarc
      }
      codenarc {
          toolVersion = "3.7.0"
          reportFormat = "text"
      }
      """.trimIndent()

    fun withBaseProject(block: TestProject.() -> Unit) =
      withTestProject {
        settingsFile.writeText(jenkinsSettings("codenarc-test"))
        block()
      }

    fun TestProject.codenarcReport(): String {
      val report = dir.resolve("build/reports/codenarc/jenkinsMain.txt")
      report.shouldBeReadable()
      return report.readText()
    }

    describe("ClassNotSerializable: class without Serializable is flagged") {
      withData(TestedGradleVersion.all) { gradleVersion ->
        withBaseProject {
          buildFile.writeText(buildFileContent)
          file("src/com/example/SomeClass.groovy").writeText(
            """
            package com.example
            class SomeClass {}
            """.trimIndent(),
          )
          runner(gradleVersion).withArguments("codenarcJenkinsMain").buildAndFail()
          codenarcReport() shouldContain "ClassNotSerializable"
        }
      }
    }

    describe("ClosureInGString: closure inside a GString is flagged") {
      withData(TestedGradleVersion.all) { gradleVersion ->
        withBaseProject {
          buildFile.writeText(buildFileContent)
          file("vars/greeting.groovy").writeText(
            $$"""def call(String name) { "Hello ${-> name}" }""",
          )
          runner(gradleVersion).withArguments("codenarcJenkinsMain").buildAndFail()
          codenarcReport() shouldContain "ClosureInGString"
        }
      }
    }

    describe("CpsCallFromNonCpsMethod: CPS method called from @NonCPS method is flagged") {
      withData(TestedGradleVersion.all) { gradleVersion ->
        withBaseProject {
          buildFile.writeText(buildFileContent)
          // CpsCallFromNonCpsMethod only fires for default-package classes (no package
          // declaration) unless cpsPackages is explicitly configured in the ruleset.
          file("src/Util.groovy").writeText(
            """
            import com.cloudbees.groovy.cps.NonCPS
            class Util implements Serializable {
                private static final long serialVersionUID = 1L
                void cpsMethod() {}
                @NonCPS
                void nonCpsMethod() { cpsMethod() }
            }
            """.trimIndent(),
          )
          runner(gradleVersion).withArguments("codenarcJenkinsMain").buildAndFail()
          codenarcReport() shouldContain "CpsCallFromNonCpsMethod"
        }
      }
    }

    describe("ExpressionInCpsMethodNotSerializable: non-Serializable local variable is flagged") {
      withData(TestedGradleVersion.all) { gradleVersion ->
        withBaseProject {
          buildFile.writeText(buildFileContent)
          file("src/com/example/Worker.groovy").writeText(
            """
            package com.example
            class SomeClass {}
            class Worker implements Serializable {
                private static final long serialVersionUID = 1L
                void run() {
                    SomeClass some = new SomeClass()
                }
            }
            """.trimIndent(),
          )
          runner(gradleVersion).withArguments("codenarcJenkinsMain").buildAndFail()
          codenarcReport() shouldContain "ExpressionInCpsMethodNotSerializable"
        }
      }
    }

    describe("ForbiddenCallInCpsMethod: sort with closure in CPS method is flagged") {
      withData(TestedGradleVersion.all) { gradleVersion ->
        withBaseProject {
          buildFile.writeText(buildFileContent)
          file("src/com/example/Sorter.groovy").writeText(
            """
            package com.example
            class Sorter implements Serializable {
                private static final long serialVersionUID = 1L
                void run() {
                    List l = [4, 1, 3]
                    l.sort { a, b -> a > b }
                }
            }
            """.trimIndent(),
          )
          runner(gradleVersion).withArguments("codenarcJenkinsMain").buildAndFail()
          codenarcReport() shouldContain "ForbiddenCallInCpsMethod"
        }
      }
    }

    describe("ObjectOverrideOnlyNonCpsMethods: toString override without @NonCPS is flagged") {
      withData(TestedGradleVersion.all) { gradleVersion ->
        withBaseProject {
          buildFile.writeText(buildFileContent)
          file("src/com/example/Step.groovy").writeText(
            """
            package com.example
            class Step implements Serializable {
                private static final long serialVersionUID = 1L
                @Override
                String toString() { return "Step" }
            }
            """.trimIndent(),
          )
          runner(gradleVersion).withArguments("codenarcJenkinsMain").buildAndFail()
          codenarcReport() shouldContain "ObjectOverrideOnlyNonCpsMethods"
        }
      }
    }

    describe("ParameterOrReturnTypeNotSerializable: non-Serializable return type is flagged") {
      withData(TestedGradleVersion.all) { gradleVersion ->
        withBaseProject {
          buildFile.writeText(buildFileContent)
          file("src/com/example/Factory.groovy").writeText(
            """
            package com.example
            class SomeClass {}
            class Factory implements Serializable {
                private static final long serialVersionUID = 1L
                SomeClass create() { return null }
            }
            """.trimIndent(),
          )
          runner(gradleVersion).withArguments("codenarcJenkinsMain").buildAndFail()
          codenarcReport() shouldContain "ParameterOrReturnTypeNotSerializable"
        }
      }
    }

    describe("no violations: Serializable class with @NonCPS toString passes") {
      withData(TestedGradleVersion.all) { gradleVersion ->
        withBaseProject {
          buildFile.writeText(buildFileContent)
          file("src/com/example/Step.groovy").writeText(
            """
            package com.example
            import com.cloudbees.groovy.cps.NonCPS
            class Step implements Serializable {
                private static final long serialVersionUID = 1L
                @NonCPS
                @Override
                String toString() { return "Step" }
            }
            """.trimIndent(),
          )
          val result = runner(gradleVersion).withArguments("codenarcJenkinsMain").build()
          result.task(":codenarcJenkinsMain") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }
        }
      }
    }

    describe("codenarcMain: succeeds without consumer config/codenarc/codenarc.xml using bundled default") {
      withData(TestedGradleVersion.all) { gradleVersion ->
        withBaseProject {
          buildFile.writeText(
            """
            plugins {
                id("com.mkobit.jenkins.pipelines.shared-library")
                codenarc
            }
            codenarc {
                toolVersion = "3.7.0"
            }
            """.trimIndent(),
          )
          // No config/codenarc/codenarc.xml — bundled default kicks in (basic + exceptions rulesets)
          file("src/com/example/Greeter.groovy").writeText(
            """
            package com.example
            class Greeter {
                String greet(String name) { return "Hello, ${'$'}name!" }
            }
            """.trimIndent(),
          )
          val result = runner(gradleVersion).withArguments("codenarcMain").build()
          result.task(":codenarcMain") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }
        }
      }
    }

    describe("codenarcMain: consumer config/codenarc/codenarc.xml takes precedence over bundled default") {
      withData(TestedGradleVersion.all) { gradleVersion ->
        withBaseProject {
          buildFile.writeText(
            """
            plugins {
                id("com.mkobit.jenkins.pipelines.shared-library")
                codenarc
            }
            codenarc {
                toolVersion = "3.7.0"
            }
            """.trimIndent(),
          )
          // Consumer config references a ruleset that does not exist. If our plugin uses the
          // bundled default instead of the consumer's file, CodeNarc would not see this reference
          // and the build would succeed. If the consumer config is correctly picked up, CodeNarc
          // throws because it cannot find the ruleset, failing the build even with ignoreFailures=true.
          file("config/codenarc/codenarc.xml").writeText(
            """
            <?xml version="1.0"?>
            <ruleset xmlns="http://codenarc.org/ruleset/1.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://codenarc.org/ruleset/1.0 http://codenarc.org/ruleset-schema.xsd">
              <ruleset-ref path="rulesets/does-not-exist.xml"/>
            </ruleset>
            """.trimIndent(),
          )
          file("src/com/example/Util.groovy").writeText(
            """
            package com.example
            class Util {}
            """.trimIndent(),
          )
          runner(gradleVersion).withArguments("codenarcMain").buildAndFail()
        }
      }
    }
  })
