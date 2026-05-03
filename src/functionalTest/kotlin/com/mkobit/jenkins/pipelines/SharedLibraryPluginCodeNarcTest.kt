package com.mkobit.jenkins.pipelines

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome
import testsupport.TestProject
import testsupport.TestedGradleVersion
import testsupport.withTestProject
import kotlin.io.path.readText
import kotlin.io.path.writeText

// Verifies that shared-library + codenarc together fire the Jenkins Enhanced Classpath Rules
// and catch violations from rulesets/jenkins.xml using the bundled codenarc-jenkins.xml resource.
// Requires Jenkins JARs on compilationClasspath — exclude with -P kotest.tags=!resolution.
@Tags("resolution")
class SharedLibraryPluginCodeNarcTest :
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
      rootProject.name = "codenarc-test"
      """.trimIndent()

    // No configFile — codenarcJenkinsMain uses the bundled resource from the plugin JAR.
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

    fun withBaseProject(block: (TestProject) -> Unit) =
      withTestProject { project ->
        project.settingsFile.writeText(settingsContent)
        block(project)
      }

    fun codenarcReport(project: TestProject) = project.dir.resolve("build/reports/codenarc/jenkinsMain.txt").readText()

    // ── ClassNotSerializable ─────────────────────────────────────────────────────

    describe("ClassNotSerializable: class without Serializable is flagged") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withBaseProject { project ->
          project.buildFile.writeText(buildFileContent)
          project.file("src/com/example/SomeClass.groovy").writeText(
            """
            package com.example
            class SomeClass {}
            """.trimIndent(),
          )
          project.runner(gradleVersion).withArguments("codenarcJenkinsMain").buildAndFail()
          codenarcReport(project) shouldContain "ClassNotSerializable"
        }
      }
    }

    // ── ClosureInGString ─────────────────────────────────────────────────────────

    describe("ClosureInGString: closure inside a GString is flagged") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withBaseProject { project ->
          project.buildFile.writeText(buildFileContent)
          project.file("vars/greeting.groovy").writeText(
            // ${'$'} produces a literal $ so Kotlin doesn't try to interpolate {-> name}
            """def call(String name) { "Hello ${'$'}{-> name}" }""",
          )
          project.runner(gradleVersion).withArguments("codenarcJenkinsMain").buildAndFail()
          codenarcReport(project) shouldContain "ClosureInGString"
        }
      }
    }

    // ── CpsCallFromNonCpsMethod ───────────────────────────────────────────────────

    describe("CpsCallFromNonCpsMethod: CPS method called from @NonCPS method is flagged") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withBaseProject { project ->
          project.buildFile.writeText(buildFileContent)
          // CpsCallFromNonCpsMethod only fires for default-package classes (no package
          // declaration) unless cpsPackages is explicitly configured in the ruleset.
          project.file("src/Util.groovy").writeText(
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
          project.runner(gradleVersion).withArguments("codenarcJenkinsMain").buildAndFail()
          codenarcReport(project) shouldContain "CpsCallFromNonCpsMethod"
        }
      }
    }

    // ── ExpressionInCpsMethodNotSerializable ─────────────────────────────────────

    describe("ExpressionInCpsMethodNotSerializable: non-Serializable local variable is flagged") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withBaseProject { project ->
          project.buildFile.writeText(buildFileContent)
          project.file("src/com/example/Worker.groovy").writeText(
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
          project.runner(gradleVersion).withArguments("codenarcJenkinsMain").buildAndFail()
          codenarcReport(project) shouldContain "ExpressionInCpsMethodNotSerializable"
        }
      }
    }

    // ── ForbiddenCallInCpsMethod ─────────────────────────────────────────────────

    describe("ForbiddenCallInCpsMethod: sort with closure in CPS method is flagged") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withBaseProject { project ->
          project.buildFile.writeText(buildFileContent)
          project.file("src/com/example/Sorter.groovy").writeText(
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
          project.runner(gradleVersion).withArguments("codenarcJenkinsMain").buildAndFail()
          codenarcReport(project) shouldContain "ForbiddenCallInCpsMethod"
        }
      }
    }

    // ── ObjectOverrideOnlyNonCpsMethods ─────────────────────────────────────────

    describe("ObjectOverrideOnlyNonCpsMethods: toString override without @NonCPS is flagged") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withBaseProject { project ->
          project.buildFile.writeText(buildFileContent)
          project.file("src/com/example/Step.groovy").writeText(
            """
            package com.example
            class Step implements Serializable {
                private static final long serialVersionUID = 1L
                @Override
                String toString() { return "Step" }
            }
            """.trimIndent(),
          )
          project.runner(gradleVersion).withArguments("codenarcJenkinsMain").buildAndFail()
          codenarcReport(project) shouldContain "ObjectOverrideOnlyNonCpsMethods"
        }
      }
    }

    // ── ParameterOrReturnTypeNotSerializable ─────────────────────────────────────

    describe("ParameterOrReturnTypeNotSerializable: non-Serializable return type is flagged") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withBaseProject { project ->
          project.buildFile.writeText(buildFileContent)
          project.file("src/com/example/Factory.groovy").writeText(
            """
            package com.example
            class SomeClass {}
            class Factory implements Serializable {
                private static final long serialVersionUID = 1L
                SomeClass create() { return null }
            }
            """.trimIndent(),
          )
          project.runner(gradleVersion).withArguments("codenarcJenkinsMain").buildAndFail()
          codenarcReport(project) shouldContain "ParameterOrReturnTypeNotSerializable"
        }
      }
    }

    // ── Clean source passes ──────────────────────────────────────────────────────

    describe("no violations: Serializable class with @NonCPS toString passes") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withBaseProject { project ->
          project.buildFile.writeText(buildFileContent)
          project.file("src/com/example/Step.groovy").writeText(
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
          val result = project.runner(gradleVersion).withArguments("codenarcJenkinsMain").build()
          result.task(":codenarcJenkinsMain")!!.outcome shouldBe TaskOutcome.SUCCESS
        }
      }
    }
  })
