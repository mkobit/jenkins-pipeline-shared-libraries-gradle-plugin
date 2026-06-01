package com.mkobit.jenkins.pipelines

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import org.gradle.testkit.runner.TaskOutcome
import testsupport.gradle.TestProject
import testsupport.gradle.TestedGradleVersion
import testsupport.gradle.withTestProject
import testsupport.jenkins.jenkinsSettings
import kotlin.io.path.writeText

class SharedLibraryPluginSourceTest :
  DescribeSpec({
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
      withData(TestedGradleVersion.all) { gradleVersion ->
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
          result.task(":compileGroovy") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }
        }
      }
    }

    describe("invalid Groovy in src/ fails compilation") {
      withData(TestedGradleVersion.all) { gradleVersion ->
        withSharedLibraryProject(configure = {
          file("src/com/example/Bad.groovy").writeText("class { not valid groovy }")
        }) {
          val result = runner(gradleVersion).withArguments("compileGroovy").buildAndFail()
          result.task(":compileGroovy") shouldNotBeNull { outcome shouldBe TaskOutcome.FAILED }
        }
      }
    }

    describe("invalid Groovy in vars/ fails compilation") {
      withData(TestedGradleVersion.all) { gradleVersion ->
        withSharedLibraryProject(configure = {
          file("vars/badStep.groovy").writeText("def call( { unclosed paren and brace")
        }) {
          val result = runner(gradleVersion).withArguments("compileGroovy").buildAndFail()
          result.task(":compileGroovy") shouldNotBeNull { outcome shouldBe TaskOutcome.FAILED }
        }
      }
    }

    describe("sourcesJar assembles a JAR of the main source") {
      withData(TestedGradleVersion.all) { gradleVersion ->
        withSharedLibraryProject(configure = {
          file("src/com/example/Lib.groovy").writeText("package com.example; class Lib {}")
        }) {
          val result = runner(gradleVersion).withArguments("sourcesJar").build()
          result.task(":sourcesJar") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }
        }
      }
    }

    describe("groovydocJar assembles a Groovydoc JAR") {
      withData(TestedGradleVersion.all) { gradleVersion ->
        withSharedLibraryProject(configure = {
          file("src/com/example/Lib.groovy").writeText(
            "package com.example; /** Documented. */ class Lib {}",
          )
        }) {
          val result = runner(gradleVersion).withArguments("groovydocJar").build()
          result.task(":groovydocJar") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }
        }
      }
    }

    describe("Jenkins API types usable in main library source") {
      withData(TestedGradleVersion.all) { gradleVersion ->
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
          result.task(":compileGroovy") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }
        }
      }
    }

    describe("unit test suite runs JenkinsPipelineUnit tests") {
      withData(TestedGradleVersion.all) { gradleVersion ->
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
          result.task(":test") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }
        }
      }
    }

    describe("running test does not trigger integrationTest") {
      withData(TestedGradleVersion.all) { gradleVersion ->
        withSharedLibraryProject {
          val result = runner(gradleVersion).withArguments("test", "--dry-run").build()
          result.output shouldNotContain ":integrationTest"
        }
      }
    }

    xdescribe("Spock integrationTest with sandbox=true (known failure: groovy-all conflict)") {
      // groovyAllRuntime injects groovy-all:2.4.x alongside Spock's groovy:3.x at JVM runtime.
      // CPS transform fails: Cannot reference 'toArray' before supertype constructor has been called.
      // Remove xdescribe when Jenkins upgrades embedded Groovy beyond 2.4.
      // Tracked: https://github.com/jenkinsci/jenkins/issues/19976
      //          https://issues.jenkins.io/browse/JENKINS-51823
      withData(TestedGradleVersion.all) { gradleVersion ->
        withSharedLibraryProject(configure = {
          buildFile.writeText(
            """
            plugins {
                id("com.mkobit.jenkins.pipelines.shared-library")
            }
            dependencies {
                integrationTestImplementation("org.spockframework:spock-core:2.3-groovy-3.0")
            }
            """.trimIndent(),
          )
          file("vars/sandboxStep.groovy").writeText("def call() { echo 'sandbox step' }")
          file("test/integration/groovy/SandboxStepSpec.groovy").writeText(
            """
            import org.jvnet.hudson.test.JenkinsRule
            import org.junit.Rule
            import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
            import org.jenkinsci.plugins.workflow.job.WorkflowJob
            import spock.lang.Specification
            class SandboxStepSpec extends Specification {
              @Rule JenkinsRule jenkins = new JenkinsRule()
              def 'sandboxStep runs with sandbox enabled'() {
                given:
                def job = jenkins.createProject(WorkflowJob)
                job.definition = new CpsFlowDefinition('sandboxStep()', true)
                expect:
                jenkins.assertLogContains('sandbox step', jenkins.buildAndAssertSuccess(job))
              }
            }
            """.trimIndent(),
          )
        }) {
          runner(gradleVersion).withArguments("integrationTest").buildAndFail()
        }
      }
    }
  })
