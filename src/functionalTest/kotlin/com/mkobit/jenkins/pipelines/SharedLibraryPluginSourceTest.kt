package com.mkobit.jenkins.pipelines

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import org.gradle.testkit.runner.TaskOutcome
import testsupport.Resolution
import testsupport.TestProject
import testsupport.TestedGradleVersion
import testsupport.jenkinsSettings
import testsupport.withTestProject
import kotlin.io.path.writeText

/**
 * Source compilation and unit test execution tests require the Jenkins Maven repo on first run.
 * Exclude from fast PR checks with `-P kotest.tags=!Resolution`.
 */
class SharedLibraryPluginSourceTest :
  DescribeSpec({
    tags(Resolution)

    fun withSharedLibraryProject(
      configure: TestProject.() -> Unit = {},
      block: TestProject.() -> Unit,
    ) = withTestProject {
      settingsFile.writeText(jenkinsSettings("source-test"))
      buildFile.writeText(
        """
        plugins {
            id("com.mkobit.jenkins.pipelines.shared-library")
        }
        """.trimIndent(),
      )
      configure()
      block()
    }

    describe("main Groovy source in src/ compiles") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withSharedLibraryProject(configure = {
          file("src/com/example/Lib.groovy").writeText(
            """
            package com.example
            class Lib {
              String greet(String name) { "Hello, ${'$'}name" }
            }
            """.trimIndent(),
          )
        }) {
          val result = runner(gradleVersion).withArguments("compileGroovy").build()
          result.task(":compileGroovy")!!.outcome shouldBe TaskOutcome.SUCCESS
        }
      }
    }

    describe("invalid Groovy in src/ fails compilation") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withSharedLibraryProject(configure = {
          file("src/com/example/Bad.groovy").writeText("class { not valid groovy }")
        }) {
          val result = runner(gradleVersion).withArguments("compileGroovy").buildAndFail()
          result.task(":compileGroovy")!!.outcome shouldBe TaskOutcome.FAILED
        }
      }
    }

    describe("invalid Groovy in vars/ fails compilation") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withSharedLibraryProject(configure = {
          file("vars/badStep.groovy").writeText("def call( { unclosed paren and brace")
        }) {
          val result = runner(gradleVersion).withArguments("compileGroovy").buildAndFail()
          result.task(":compileGroovy")!!.outcome shouldBe TaskOutcome.FAILED
        }
      }
    }

    describe("sourcesJar assembles a JAR of the main source") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withSharedLibraryProject(configure = {
          file("src/com/example/Lib.groovy").writeText("package com.example; class Lib {}")
        }) {
          val result = runner(gradleVersion).withArguments("sourcesJar").build()
          result.task(":sourcesJar")!!.outcome shouldBe TaskOutcome.SUCCESS
        }
      }
    }

    describe("groovydocJar assembles a Groovydoc JAR") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withSharedLibraryProject(configure = {
          file("src/com/example/Lib.groovy").writeText(
            "package com.example; /** Documented. */ class Lib {}",
          )
        }) {
          val result = runner(gradleVersion).withArguments("groovydocJar").build()
          result.task(":groovydocJar")!!.outcome shouldBe TaskOutcome.SUCCESS
        }
      }
    }

    describe("Jenkins API types usable in main library source") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withSharedLibraryProject(configure = {
          file("src/com/example/JenkinsLib.groovy").writeText(
            """
            package com.example
            import hudson.model.Item
            class JenkinsLib {
              Item item
            }
            """.trimIndent(),
          )
        }) {
          val result = runner(gradleVersion).withArguments("compileGroovy").build()
          result.task(":compileGroovy")!!.outcome shouldBe TaskOutcome.SUCCESS
        }
      }
    }

    describe("unit test suite runs JenkinsPipelineUnit tests") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withSharedLibraryProject(configure = {
          // Spock brings Groovy 3 onto the compile classpath; groovy-all exclusion is
          // applied by the plugin so Jenkins' bundled Groovy 2.4 does not conflict.
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
          file("vars/myStep.groovy").writeText("def call() { echo 'hello from myStep' }")
          file("test/unit/groovy/MyStepSpec.groovy").writeText(
            """
            import com.lesfurets.jenkins.unit.BasePipelineTest
            import spock.lang.Specification
            class MyStepSpec extends Specification {
              BasePipelineTest base = new BasePipelineTest() {}
              def setup() { base.scriptRoots += 'vars'; base.setUp() }
              def "myStep executes without error"() {
                when: base.loadScript('vars/myStep.groovy').call()
                then: noExceptionThrown()
              }
            }
            """.trimIndent(),
          )
        }) {
          val result = runner(gradleVersion).withArguments("test").build()
          result.task(":test")!!.outcome shouldBe TaskOutcome.SUCCESS
        }
      }
    }

    describe("running test does not trigger integrationTest") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withSharedLibraryProject {
          val result = runner(gradleVersion).withArguments("test", "--dry-run").build()
          result.output shouldNotContain ":integrationTest"
        }
      }
    }
  })
