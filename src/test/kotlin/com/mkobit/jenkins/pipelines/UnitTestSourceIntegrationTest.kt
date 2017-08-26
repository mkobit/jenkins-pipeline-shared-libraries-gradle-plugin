package com.mkobit.jenkins.pipelines

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import testsupport.build
import testsupport.writeRelativeFile
import java.io.File
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
internal class UnitTestSourceIntegrationTest {

  private lateinit var projectDir: File

  @BeforeEach
  internal fun setUp() {
    projectDir = createTempDir().apply { deleteOnExit() }
  }

  @ParameterizedTest(name = "version {0}")
  @MethodSource("jenkinsPipelineUnitTestData")
  internal fun `can write unit tests using JenkinsPipelineUnit`(version: String, pipeline: String, groovyTestFile: String) {
    projectDir.writeRelativeFile(fileName = "build.gradle") {
      groovyBuildScript() +
        """
sharedLibrary {
  pipelineTestUnitVersion = "$version"
}
"""
    }

    projectDir.writeRelativeFile(fileName = "example.jenkins") {
      pipeline
    }

    projectDir.writeRelativeFile("test", "unit", "groovy", "com", "mkobit", fileName = "JenkinsPipelineUnitUsageTest.groovy") {
      groovyTestFile
    }

    val buildResult: BuildResult = build(projectDir, "test", "-s", "-i")

    val task = buildResult.task(":test")
    assertThat(task).isNotNull()
    assertThat(task?.outcome)
      .describedAs("test task outcome")
      .withFailMessage("Build output: ${buildResult.output}")
      .isNotNull()
      .isEqualTo(TaskOutcome.SUCCESS)
  }

  fun jenkinsPipelineUnitTestData(): Stream<Arguments> {
    val exampleJenkins = """def execute() {
  node {
    echo "We in the pipeline now!"
  }
}

return this
"""

    return Stream.of(
      Arguments.of("1.0", exampleJenkins, """
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
"""),
      Arguments.of("1.1", exampleJenkins, """
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
    runScript("example.jenkins")
    printCallStack()
    assertJobStatusSuccess()
  }
}
""")
    )
  }
}
