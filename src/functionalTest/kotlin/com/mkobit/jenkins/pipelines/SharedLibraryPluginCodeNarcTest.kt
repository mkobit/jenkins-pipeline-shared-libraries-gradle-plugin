package com.mkobit.jenkins.pipelines

import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome
import testsupport.TestProjectBuilder
import testsupport.TestedGradleVersion

// Verifies that shared-library + codenarc together fire the Jenkins Enhanced Classpath Rules
// and catch violations from rulesets/jenkins.xml.
// Requires Jenkins JARs on compilationClasspath — exclude with -P kotest.tags=!resolution.
@Tags("resolution")
class SharedLibraryPluginCodeNarcTest :
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
          rootProject.name = "codenarc-test"
          """.trimIndent(),
        )
      }

    val buildFileContent =
      """
      plugins {
          id("com.mkobit.jenkins.pipelines.shared-library")
          codenarc
      }
      codenarc {
          toolVersion = "3.7.0"
          reportFormat = "text"
          configFile = file("codenarc.xml")
      }
      """.trimIndent()

    val codeNarcXml =
      """
      <?xml version="1.0"?>
      <ruleset xmlns="http://codenarc.org/ruleset/1.0"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://codenarc.org/ruleset/1.0 http://codenarc.org/ruleset-schema.xsd">
        <ruleset-ref path="rulesets/jenkins.xml"/>
      </ruleset>
      """.trimIndent()

    fun codenarcReport(project: TestProjectBuilder) = project.dir.resolve("build/reports/codenarc/main.txt").readText()

    // ── ClassNotSerializable ─────────────────────────────────────────────────────

    describe("ClassNotSerializable: class without Serializable is flagged") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        baseProject()
          .apply {
            buildFile.writeText(buildFileContent)
            file("codenarc.xml").writeText(codeNarcXml)
            // Violation: SomeClass does not implement Serializable
            file("src/com/example/SomeClass.groovy").writeText(
              """
              package com.example
              class SomeClass {}
              """.trimIndent(),
            )
          }.use { project ->
            project.runner(gradleVersion).withArguments("codenarcMain").buildAndFail()
            codenarcReport(project) shouldContain "ClassNotSerializable"
          }
      }
    }

    // ── ClosureInGString ─────────────────────────────────────────────────────────

    describe("ClosureInGString: closure inside a GString is flagged") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        baseProject()
          .apply {
            buildFile.writeText(buildFileContent)
            file("codenarc.xml").writeText(codeNarcXml)
            // Violation: ${-> x} is a closure inside a GString
            file("vars/greeting.groovy").writeText(
              // ${'$'} produces a literal $ so Kotlin doesn't try to interpolate {-> name}
              """def call(String name) { "Hello ${'$'}{-> name}" }""",
            )
          }.use { project ->
            project.runner(gradleVersion).withArguments("codenarcMain").buildAndFail()
            codenarcReport(project) shouldContain "ClosureInGString"
          }
      }
    }

    // ── CpsCallFromNonCpsMethod ───────────────────────────────────────────────────

    describe("CpsCallFromNonCpsMethod: CPS method called from @NonCPS method is flagged") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        baseProject()
          .apply {
            buildFile.writeText(buildFileContent)
            file("codenarc.xml").writeText(codeNarcXml)
            file("src/com/example/Util.groovy").writeText(
              """
              package com.example
              import com.cloudbees.groovy.cps.NonCPS
              class Util implements Serializable {
                  private static final long serialVersionUID = 1L
                  void cpsMethod() {}
                  @NonCPS
                  void nonCpsMethod() { cpsMethod() }
              }
              """.trimIndent(),
            )
          }.use { project ->
            project.runner(gradleVersion).withArguments("codenarcMain").buildAndFail()
            codenarcReport(project) shouldContain "CpsCallFromNonCpsMethod"
          }
      }
    }

    // ── ExpressionInCpsMethodNotSerializable ─────────────────────────────────────

    describe("ExpressionInCpsMethodNotSerializable: non-Serializable local variable is flagged") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        baseProject()
          .apply {
            buildFile.writeText(buildFileContent)
            file("codenarc.xml").writeText(codeNarcXml)
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
          }.use { project ->
            project.runner(gradleVersion).withArguments("codenarcMain").buildAndFail()
            codenarcReport(project) shouldContain "ExpressionInCpsMethodNotSerializable"
          }
      }
    }

    // ── ForbiddenCallInCpsMethod ─────────────────────────────────────────────────

    describe("ForbiddenCallInCpsMethod: sort with closure in CPS method is flagged") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        baseProject()
          .apply {
            buildFile.writeText(buildFileContent)
            file("codenarc.xml").writeText(codeNarcXml)
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
          }.use { project ->
            project.runner(gradleVersion).withArguments("codenarcMain").buildAndFail()
            codenarcReport(project) shouldContain "ForbiddenCallInCpsMethod"
          }
      }
    }

    // ── ObjectOverrideOnlyNonCpsMethods ─────────────────────────────────────────

    describe("ObjectOverrideOnlyNonCpsMethods: toString override without @NonCPS is flagged") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        baseProject()
          .apply {
            buildFile.writeText(buildFileContent)
            file("codenarc.xml").writeText(codeNarcXml)
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
          }.use { project ->
            project.runner(gradleVersion).withArguments("codenarcMain").buildAndFail()
            codenarcReport(project) shouldContain "ObjectOverrideOnlyNonCpsMethods"
          }
      }
    }

    // ── ParameterOrReturnTypeNotSerializable ─────────────────────────────────────

    describe("ParameterOrReturnTypeNotSerializable: non-Serializable return type is flagged") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        baseProject()
          .apply {
            buildFile.writeText(buildFileContent)
            file("codenarc.xml").writeText(codeNarcXml)
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
          }.use { project ->
            project.runner(gradleVersion).withArguments("codenarcMain").buildAndFail()
            codenarcReport(project) shouldContain "ParameterOrReturnTypeNotSerializable"
          }
      }
    }

    // ── Clean source passes ──────────────────────────────────────────────────────

    describe("no violations: Serializable class with @NonCPS toString passes") {
      withData(TestedGradleVersion.entries) { gradleVersion ->
        baseProject()
          .apply {
            buildFile.writeText(buildFileContent)
            file("codenarc.xml").writeText(codeNarcXml)
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
          }.use { project ->
            val result = project.runner(gradleVersion).withArguments("codenarcMain").build()
            result.task(":codenarcMain")!!.outcome shouldBe TaskOutcome.SUCCESS
          }
      }
    }
  })
