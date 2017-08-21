package com.mkobit.jenkins.pipelines

import org.assertj.core.api.Assertions
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import testsupport.writeRelativeFile
import java.io.File

@Tag("integration")
internal class UnitTestSourceIntegrationTest {

  private lateinit var projectDir: File

  @BeforeEach
  internal fun setUp() {
    projectDir = createTempDir().apply { deleteOnExit() }
  }

  @Test
  internal fun `can write unit tests using JenkinsPipelineUnit`() {
    projectDir.writeRelativeFile(fileName = "build.gradle") {
      groovyBuildScript() +
        """
sharedLibrary {
  pipelineTestUnitVersion = "1.0"
}
"""
    }

    projectDir.writeRelativeFile(fileName = "example.jenkins") {
      """
def execute() {
  node {
    echo "We in the pipeline now!"
  }
}

return this
"""
    }

    projectDir.writeRelativeFile("test", "unit", "groovy", "com", "mkobit", fileName = "JenkinsPipelineUnitUsageTest.groovy") {
      """
package com.mkobit

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import com.lesfurets.jenkins.unit.BasePipelineTest

class JenkinsPipelineUnitUsageTest extends BasePipelineTest {

  @Override
  @Before
  void setUp() throws Exception {
    super.setUp()
  }

  @Test
  void canExecutePipelineJob() {
    final script = loadScript("example.jenkins")
    script.execute()
    printCallStack()
  }
}
"""
    }

    val buildResult: BuildResult = GradleRunner.create()
      .withPluginClasspath()
      .withArguments("test", "-s", "-i")
      .withProjectDir(projectDir)
      .build()

    val task = buildResult.task(":test")
    Assertions.assertThat(task).isNotNull()
    Assertions.assertThat(task?.outcome)
      .describedAs("test task outcome")
      .withFailMessage("Build output: ${buildResult.output}")
      .isNotNull()
      .isEqualTo(TaskOutcome.SUCCESS)
  }

}
