package com.mkobit.jenkins.pipelines

import com.mkobit.gradle.test.kotlin.testkit.runner.build
import com.mkobit.gradle.test.kotlin.testkit.runner.info
import com.mkobit.gradle.test.kotlin.testkit.runner.quiet
import org.gradle.api.artifacts.Configuration
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestTemplate
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.contains
import strikt.assertions.endsWith
import strikt.assertions.isEmpty
import strikt.assertions.isNotEmpty
import strikt.assertions.isNotNull
import strikt.assertions.none
import strikt.assertions.succeeded
import strikt.gradle.testkit.isSuccess
import strikt.gradle.testkit.output
import strikt.gradle.testkit.task
import strikt.gradle.testkit.tasks
import testsupport.junit.ForGradleVersions
import testsupport.junit.GradleProject
import testsupport.junit.IntelliJSupport
import testsupport.junit.Issue
import testsupport.junit.NotImplementedYet
import testsupport.strikt.allOf
import testsupport.strikt.anyOf
import java.util.regex.Pattern

@ForGradleVersions
internal class IntegrationTestSourceIntegrationTest {

  @TestTemplate
  internal fun `can execute dependencies task`(@GradleProject(["projects", "only-plugins-block"]) gradleRunner: GradleRunner) {
    expectCatching {
      gradleRunner.build("dependencies")
    }.succeeded()
  }

  @TestTemplate
  internal fun `integrationTest compile classpath does not contain any HPI or JPI artifacts`(@GradleProject(["projects", "show-configuration-states"]) gradleRunner: GradleRunner) {
    val buildResult = gradleRunner.apply {
      quiet = true
    }.build("showResolvedIntegrationTestCompileClasspathArtifacts")

    expectThat(buildResult) {
      output.get { trim().split(System.lineSeparator()) }
        .isNotEmpty()
        .none { endsWith("@hpi") }
        .none { endsWith("@jpi") }
        .all { endsWith("@jar") }
    }
  }

  @TestTemplate
  internal fun `integrationTest runtimeOnly configuration contains only HPI, JPI, and WAR artifacts`(@GradleProject(["projects", "show-configuration-states"]) gradleRunner: GradleRunner) {
    val buildResult = gradleRunner.build("--quiet", "showResolvedIntegrationTestRuntimeOnlyArtifacts")

    expectThat(buildResult) {
      output.get { trim().split(System.lineSeparator()) }
        .isNotEmpty()
        .all {
          anyOf {
            endsWith("@hpi")
            endsWith("@jpi")
            allOf {
              contains("org.jenkins-ci.main:jenkins-war")
              endsWith("@war")
            }
          }
        }
    }
  }

  @TestTemplate
  internal fun `JenkinsPipelineUnit is not available in the integrationTest compile classpath`(@GradleProject(["projects", "show-configuration-states"]) gradleRunner: GradleRunner) {
    val buildResult = gradleRunner.build("--quiet", "showResolvedIntegrationTestCompileClasspathArtifacts")

    expectThat(buildResult) {
      output.get { trim().split(System.lineSeparator()) }
        .isNotEmpty()
        .all {
          not {
            anyOf {
              contains("com.lesfurets:")
              allOf {
                // Jenkins Pipeline Unit group: com.lesfurets
                contains("org.jenkins-ci.main:jenkins-war")
                // Jenkins Pipeline Unit module: jenkins-pipeline-unit
                contains(":jenkins-pipeline-unit:")
              }
            }
          }
        }
    }
  }

  @TestTemplate
  internal fun `can compile integration test sources that use Jenkins libraries`(@GradleProject(["projects", "import-jenkins-classes"]) gradleRunner: GradleRunner) {
    val buildResult = gradleRunner.apply {
      info = true
    }.build("compileIntegrationTestGroovy")

    expectThat(buildResult)
      .describedAs("integrationTestCompileGroovy task outcome")
      .task(":compileIntegrationTestGroovy")
      .isNotNull()
      .isSuccess()
  }

  @TestTemplate
  internal fun `can run tests using @JenkinsRule in integration tests`(@GradleProject(["projects", "basic-JenkinsRule-usage"]) gradleRunner: GradleRunner) {
    val buildResult = gradleRunner.apply {
      info = true
    }.build("integrationTest")

    expectThat(buildResult)
      .task(":integrationTest")
      .isNotNull()
      .isSuccess()
  }

  @TestTemplate
  @Issue("https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/23")
  internal fun `no startup exceptions for tests`(@GradleProject(["projects", "basic-JenkinsRule-usage"]) gradleRunner: GradleRunner) {
    val buildResult = gradleRunner.apply {
      info = true
    }.build("integrationTest")

    expectThat(buildResult)
      .output
      .and {
        not {
          contains("Caused: java.lang.NoClassDefFoundError: org/jenkinsci/main/modules/sshd/SshCommandFactory")
          contains("Caused: com.google.inject.ProvisionException: Unable to provision, see the following errors:")
          contains(Pattern.compile(".*\\sERROR\\s.*", Pattern.DOTALL).toRegex())
        }
      }
  }

  @TestTemplate
  internal fun `WorkflowJob can be created and executed in integration tests`(@GradleProject(["projects", "basic-WorkflowJob-usage"]) gradleRunner: GradleRunner) {
    val buildResult = gradleRunner.apply {
      info = true
    }.build("integrationTest")

    expectThat(buildResult)
      .task(":integrationTest")
      .isNotNull()
      .isSuccess()
  }

  @TestTemplate
  internal fun `can set up Global Pipeline Library and use them in an integration test`(@GradleProject(["projects", "global-library-with-generated-test-source"]) gradleRunner: GradleRunner) {
    val buildResult = gradleRunner.apply {
      info = true
    }.build("integrationTest")

    expectThat(buildResult)
      .task(":integrationTest")
      .isNotNull()
      .isSuccess()
  }

  @TestTemplate
  fun `run integrationTest repeatedly then tasks are up-to-date`(@GradleProject(["projects", "basic-groovy-library"]) gradleRunner: GradleRunner) {
    gradleRunner.apply {
      info = true
    }

    expectThat(gradleRunner.build("integrationTest")) {
      tasks(TaskOutcome.SUCCESS)
        .isNotEmpty()
      tasks(TaskOutcome.UP_TO_DATE)
        .isEmpty()
    }

    expectThat(gradleRunner.build("integrationTest")) {
      tasks(TaskOutcome.SUCCESS)
        .isEmpty()
      tasks(TaskOutcome.UP_TO_DATE)
        .isNotEmpty()
    }
  }

  @NotImplementedYet
  @Test
  @IntelliJSupport
  internal fun `pipeline resources have main sourceSet available on compile classpath for tooling assistance with IDEA`() {
  }

  @NotImplementedYet
  @Test
  @IntelliJSupport
  internal fun `can use pipeline resources in integration tests`() {
  }

  @TestTemplate
  internal fun `no configurations are resolved if no build tasks are executed`(@GradleProject(["projects", "show-configuration-states"]) gradleRunner: GradleRunner) {
    val buildResult = gradleRunner.build("--quiet", "printConfigurationStates")

    expectThat(buildResult)
      .output.get { trim().split(System.lineSeparator()) }
      .all {
        endsWith(Configuration.State.UNRESOLVED.name)
      }
  }

  @TestTemplate
  internal fun `Groovy DSL extension configuration`(@GradleProject(["projects", "gradle-configuration-groovy"]) gradleRunner: GradleRunner) {
    expectCatching {
      gradleRunner.apply {
        info = true
      }.build()
    }.succeeded()
  }

  @TestTemplate
  fun `Kotlin DSL extension configuration`(@GradleProject(["projects", "gradle-configuration-kotlin"]) gradleRunner: GradleRunner) {
    expectCatching {
      gradleRunner.apply {
        info = true
      }.build()
    }.succeeded()
  }

  @TestTemplate
  internal fun `"check" lifecycle task executes "integrationTest"`(@GradleProject(["projects", "only-plugins-block"]) gradleRunner: GradleRunner) {
    val buildResult = gradleRunner.build("check")

    expectThat(buildResult) {
      task(":test").isNotNull()
      task(":integrationTest").isNotNull()
    }
  }

  @TestTemplate
  internal fun `generated sources can be used in Java and Groovy integration tests`(@GradleProject(["projects", "basic-generated-sources-usage"]) gradleRunner: GradleRunner) {
    expectThat(gradleRunner.build("integrationTest", "--tests", "*LocalLibraryUsageFromGroovyTest"))
      .output
      .contains("com.mkobit.LocalLibraryUsageFromGroovyTest > createRetriever STARTED")

    expectThat(gradleRunner.build("integrationTest", "--tests", "*LocalLibraryUsageFromJavaTest"))
      .output
      .contains("com.mkobit.LocalLibraryUsageFromJavaTest > createRetriever STARTED")
  }

  @TestTemplate
  @Issue("https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/61")
  fun `generated sources can be consumed in a @JenkinsRule`(@GradleProject(["projects", "generated-sources-JenkinsRule-usage"]) gradleRunner: GradleRunner) {
    val buildResult = gradleRunner.build("check")

    expectThat(buildResult)
      .output
      .and {
        contains("com.mkobit.LocalLibraryJenkinsRuleUsageTest > noopTest STANDARD_ERROR")
        not { contains("Failed to save") }
        not { contains("Refusing to marshal") }
        not { contains("might be dangerous, so rejecting; see https://jenkins.io/redirect/class-filter/") }
      }
  }

  @TestTemplate
  internal fun `can integration test library code that makes use of Jenkins core and plugin classes`(@GradleProject(["projects", "global-library-using-jenkins-plugin-classes"]) gradleRunner: GradleRunner) {
    val buildResult = gradleRunner.build("integrationTest")

    expectThat(buildResult)
      .output
      .contains("com.mkobit.LibraryUsingJenkinsClassesIntegrationTest > can use Jenkins core classes STARTED")
      .contains("com.mkobit.LibraryUsingJenkinsClassesIntegrationTest > can use plugin classes STARTED")
  }

  @TestTemplate
  internal fun `@Grab in source library can be integration tested`(@GradleProject(["projects", "source-with-@grab"]) gradleRunner: GradleRunner) {
    expectCatching {
      gradleRunner.build("integrationTest")
    }.succeeded()
  }

  @NotImplementedYet
  @Test
  internal fun `integration test output for Jenkins Test Harness is in the build directory`() {
  }

  // TODO: this should be tested but may or may not be needed. I have a feeling classloader errors
  // will happen in pipeline code if those classes are available.
  @NotImplementedYet
  @Test
  internal fun `cannot use classes from main source code in integration test`() {
  }

  @NotImplementedYet
  @Test
  internal fun `can use declared plugin dependencies in integration test`() {
  }
}
