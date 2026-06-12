package com.mkobit.jenkins.pipelines

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome
import testsupport.gradle.TestProject
import testsupport.gradle.TestedGradleVersion
import testsupport.gradle.withTestProject
import testsupport.jenkins.jenkinsSettings
import testsupport.kotest.JenkinsCompat
import kotlin.io.path.writeText

class SharedLibraryPluginShStepTest :
  DescribeSpec({
    tags(JenkinsCompat)

    fun withSharedLibraryProject(block: TestProject.() -> Unit) =
      withTestProject {
        settingsFile.writeText(jenkinsSettings("sh-step-test"))
        buildFile.writeText(
          """
          plugins {
              id("com.mkobit.jenkins.pipelines.shared-library")
          }
          sharedLibrary {
              plugins {
                  plugin("org.jenkins-ci.plugins.workflow:workflow-basic-steps")
                  plugin("org.jenkins-ci.plugins.workflow:workflow-durable-task-step")
              }
          }
          """.trimIndent(),
        )
        block()
      }

    describe("Jenkins pipeline using sh and bat steps") {
      withData(TestedGradleVersion.all) { gradleVersion ->
        withSharedLibraryProject {
          file("vars/runOsCommand.groovy").writeText(
            """
            def call() {
                node {
                    if (isUnix()) {
                        // sh step relies on durable-task which is tested to be resolvable
                        echo "Executing on Unix"
                    } else {
                        echo "Executing on Windows"
                    }
                }
            }
            """.trimIndent(),
          )

          file("test/integration/java/com/example/RunOsCommandTest.java").writeText(
            """
            package com.example;

            import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
            import org.jenkinsci.plugins.workflow.job.WorkflowJob;
            import org.jenkinsci.plugins.workflow.job.WorkflowRun;
            import org.junit.jupiter.api.Test;
            import org.jvnet.hudson.test.JenkinsRule;
            import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

            @WithJenkins
            class RunOsCommandTest {
                @Test
                void executesPlatformSpecificLogic(JenkinsRule jenkins) throws Exception {
                    WorkflowJob job = jenkins.createProject(WorkflowJob.class);
                    job.setDefinition(new CpsFlowDefinition("runOsCommand()", true));
                    WorkflowRun run = jenkins.buildAndAssertSuccess(job);
                    if (java.io.File.separatorChar == '/') {
                        jenkins.assertLogContains("Executing on Unix", run);
                    } else {
                        jenkins.assertLogContains("Executing on Windows", run);
                    }
                }
            }
            """.trimIndent(),
          )

          val result = runner(gradleVersion).withArguments("integrationTest").build()
          result.task(":integrationTest") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }
        }
      }
    }
  })
