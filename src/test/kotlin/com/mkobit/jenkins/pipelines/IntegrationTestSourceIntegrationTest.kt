package com.mkobit.jenkins.pipelines

import com.mkobit.gradle.test.assertj.GradleAssertions.assertThat
import com.mkobit.gradle.test.kotlin.testkit.runner.build
import com.mkobit.gradle.test.kotlin.testkit.runner.info
import com.mkobit.gradle.test.kotlin.testkit.runner.quiet
import org.assertj.core.api.Assertions.allOf
import org.assertj.core.api.Assertions.anyOf
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.not
import org.gradle.api.artifacts.Configuration
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestTemplate
import testsupport.ForGradleVersions
import testsupport.GradleProject
import testsupport.IntelliJSupport
import testsupport.Issue
import testsupport.NotImplementedYet
import testsupport.condition
import testsupport.softlyAssert
import java.util.regex.Pattern
import org.junit.jupiter.params.provider.Arguments.of as argumentsOf

@ForGradleVersions
internal class IntegrationTestSourceIntegrationTest {

  @TestTemplate
  internal fun `can execute dependencies task`(@GradleProject(["projects", "only-plugins-block"]) gradleRunner: GradleRunner) {
    assertThatCode {
      gradleRunner.build("dependencies")
    }.doesNotThrowAnyException()
  }

  @TestTemplate
  internal fun `integrationTest compile classpath does not contain any HPI or JPI artifacts`(@GradleProject(["projects", "show-configuration-states"]) gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.apply {
      quiet = true
    }.build("showResolvedIntegrationTestCompileClasspathArtifacts")

    softlyAssert {
      assertThat(buildResult.output.trim().split(System.lineSeparator())).isNotEmpty.allSatisfy {
        assertThat(it)
          .doesNotEndWith("@hpi")
          .doesNotEndWith("@jpi")
          .endsWith("@jar")
      }
    }
  }

  @TestTemplate
  internal fun `integrationTest runtimeOnly configuration contains only HPI, JPI, and WAR artifacts`(@GradleProject(["projects", "show-configuration-states"]) gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.build("--quiet", "showResolvedIntegrationTestRuntimeOnlyArtifacts")

    softlyAssert(buildResult) {
      outputSatisfies {
        assertThat(it.trim().split(System.lineSeparator()))
          .isNotEmpty
          .allSatisfy {
            val hpi = condition<String>(".hpi extension") { it.endsWith("@hpi") }
            val jpi = condition<String>(".jpi extension") { it.endsWith("@jpi") }
            val war = condition<String>(".war extension") { it.endsWith("@war") }
            val jenkinsWar = condition<String>("Jenkins WAR group and module") { it.contains("org.jenkins-ci.main:jenkins-war") }
            assertThat(it)
              .has(anyOf(allOf(war, jenkinsWar), hpi, jpi))
          }
      }
    }
  }

  @TestTemplate
  internal fun `JenkinsPipelineUnit is not available in the integrationTest compile classpath`(@GradleProject(["projects", "show-configuration-states"]) gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.build("--quiet", "showResolvedIntegrationTestCompileClasspathArtifacts")

    softlyAssert(buildResult) {
      outputSatisfies {
        assertThat(it.trim().split(System.lineSeparator()))
          .isNotEmpty
          .allSatisfy {
            val group = condition<String>("Jenkins Pipeline Unit group: com.lesfurets") { it.contains("com.lesfurets:") }
            val module = condition<String>("Jenkins Pipeline Unit module: jenkins-pipeline-unit") { it.contains(":jenkins-pipeline-unit:") }
            assertThat(it).has(not(anyOf(group, module)))
          }
      }
    }
  }

  @TestTemplate
  internal fun `can compile integration test sources that use Jenkins libraries`(@GradleProject(["projects", "import-jenkins-classes"]) gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.apply {
      info = true
    }.build("compileIntegrationTestGroovy")

    assertThat(buildResult)
      .describedAs("integrationTestCompileGroovy task outcome")
      .withFailMessage("Build output: %s", buildResult.output)
      .hasTaskSuccessAtPath(":compileIntegrationTestGroovy")
  }

  @TestTemplate
  internal fun `can run tests using @JenkinsRule in integration tests`(@GradleProject(["projects", "basic-JenkinsRule-usage"]) gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.apply {
      info = true
    }.build("integrationTest")

    assertThat(buildResult)
      .withFailMessage("Build output: %s", buildResult.output)
      .hasTaskSuccessAtPath(":integrationTest")
  }

  @TestTemplate
  @Issue("https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/23")
  internal fun `no startup exceptions for tests`(@GradleProject(["projects", "basic-JenkinsRule-usage"]) gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.apply {
      info = true
    }.build("integrationTest")

    assertThat(buildResult)
      .outputDoesNotContain("Caused: java.lang.NoClassDefFoundError: org/jenkinsci/main/modules/sshd/SshCommandFactory")
      .outputDoesNotContain("Caused: com.google.inject.ProvisionException: Unable to provision, see the following errors:")
      .outputDoesNotMatch(Pattern.compile(".*\\sERROR\\s.*", Pattern.DOTALL))
  }

  @TestTemplate
  internal fun `WorkflowJob can be created and executed in integration tests`(@GradleProject(["projects", "basic-WorkflowJob-usage"]) gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.apply {
      info = true
    }.build("integrationTest")

    assertThat(buildResult)
      .withFailMessage("Build output: %s", buildResult.output)
      .hasTaskSuccessAtPath(":integrationTest")
  }

  @TestTemplate
  internal fun `can set up Global Pipeline Library and use them in an integration test`(@GradleProject(["projects", "global-library-with-generated-test-source"]) gradleRunner: GradleRunner) {
    val buildResult: BuildResult = gradleRunner.apply {
      info = true
    }.build("integrationTest")

    assertThat(buildResult)
      .withFailMessage("Build output: %s", buildResult.output)
      .hasTaskSuccessAtPath(":integrationTest")
  }

  @TestTemplate
  fun `run integrationTest repeatedly then tasks are up-to-date`(@GradleProject(["projects", "basic-groovy-library"]) gradleRunner: GradleRunner) {
    gradleRunner.apply {
      info = true
    }

    assertThat(gradleRunner.build("integrationTest"))
      .tasksWithOutcomeSatisfy(TaskOutcome.SUCCESS) {
        assertThat(it).isNotEmpty
      }.tasksWithOutcomeSatisfy(TaskOutcome.UP_TO_DATE) {
        assertThat(it).isEmpty()
      }

    assertThat(gradleRunner.build("integrationTest"))
      .tasksWithOutcomeSatisfy(TaskOutcome.SUCCESS) {
        assertThat(it).isEmpty()
      }.tasksWithOutcomeSatisfy(TaskOutcome.UP_TO_DATE) {
        assertThat(it).isNotEmpty
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
    val buildResult: BuildResult = gradleRunner.build("--quiet", "printConfigurationStates")


    assertThat(buildResult).outputSatisfies {
      assertThat(it.trim().split(System.lineSeparator())).allSatisfy {
        assertThat(it).endsWith(Configuration.State.UNRESOLVED.name)
      }
    }
  }

  @TestTemplate
  fun `Groovy DSL extension configuration`(@GradleProject(["projects", "gradle-configuration-groovy"]) gradleRunner: GradleRunner) {
    assertThatCode {
      gradleRunner.apply {
        info = true
      }.build()
    }.doesNotThrowAnyException()
  }

  @Disabled("This example works in a local Gradle project, but not with Gradle Test Kit. See https://github.com/gradle/kotlin-dsl/issues/492")
  @TestTemplate
  fun `Kotlin DSL extension configuration`(@GradleProject(["projects", "gradle-configuration-kotlin"]) gradleRunner: GradleRunner) {
    assertThatCode {
      gradleRunner.apply {
        info = true
      }.build()
    }.doesNotThrowAnyException()
  }

  @TestTemplate
  internal fun `"check" lifecycle task executes "integrationTest"`(@GradleProject(["projects", "only-plugins-block"]) gradleRunner: GradleRunner) {
    val buildResult = gradleRunner.build("check")

    assertThat(buildResult)
      .hasTaskAtPath(":test")
      .hasTaskAtPath(":integrationTest")
  }

  @TestTemplate
  internal fun `generated sources can be used in Java and Groovy integration tests`(@GradleProject(["projects", "basic-generated-sources-usage"]) gradleRunner: GradleRunner) {
    val buildResult = gradleRunner.apply {
      info = true
    }.build("check")

    assertThat(buildResult)
      .hasTaskSuccessAtPath(":compileIntegrationTestGroovy")
  }

  @Disabled("https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/61")
  @TestTemplate
  @Issue("https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/61")
  fun `generated sources can be consumed in a @JenkinsRule`(@GradleProject(["projects", "generated-sources-JenkinsRule-usage"]) gradleRunner: GradleRunner) {
    val buildResult = gradleRunner.build("check")

    assertThat(buildResult)
      .outputDoesNotContain("Failed to save")
      .outputDoesNotContain("Refusing to marshal")
  }

  @TestTemplate
  internal fun `can integration test library code that makes use of Jenkins core and plugin classes`(@GradleProject(["projects", "global-library-using-jenkins-plugin-classes"]) gradleRunner: GradleRunner) {
    val buildResult = gradleRunner.build("integrationTest")
    assertThat(buildResult)
      .outputContains("com.mkobit.LibraryUsingJenkinsClassesIntegrationTest > can use Jenkins core classes STARTED")
      .outputContains("com.mkobit.LibraryUsingJenkinsClassesIntegrationTest > can use plugin classes STARTED")
  }

  @TestTemplate
  internal fun `@Grab in source library can be integration tested`(@GradleProject(["projects", "source-with-@grab"]) gradleRunner: GradleRunner) {
    gradleRunner.build("integrationTest")
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
