package com.mkobit.jenkins.pipelines

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import testsupport.NotImplementedYet
import testsupport.resourceText
import testsupport.writeRelativeFile
import java.io.File

// TODO: do better assertions on output from tasks
@Tag("integration")
internal class SharedLibraryPluginIntegrationTest {

  private lateinit var projectDir: File

  @BeforeEach
  internal fun setUp() {
    projectDir = createTempDir().apply { deleteOnExit() }
  }

  // TODO: test both groovy and kotlin usages
  @Test
  internal fun `main Groovy code is compiled`() {
    projectDir.writeRelativeFile(fileName = "build.gradle") {
      groovyBuildScript()
    }
    projectDir.writeRelativeFile("src", "com", "mkobit", fileName = "MyLib.groovy") {
      """
package com.mkobit
class MyLib {
  int add(int a, int b) {
    return a + b
  }
}
"""
    }
    projectDir.writeRelativeFile("test", "unit", "groovy", "com", "mkobit", fileName = "MyLibTest.groovy") {
      """
package com.mkobit

import org.junit.Assert
import org.junit.Test

class MyLibTest {

  @Test
  void checkAddition() {
    def myLib = new MyLib()
    Assert.assertEquals(3, myLib.add(1, 2))
  }
}
"""
    }

    val buildResult: BuildResult = GradleRunner.create()
      .withPluginClasspath()
      .withArguments("compileGroovy")
      .withProjectDir(projectDir)
      .build()

    val task = buildResult.task(":compileGroovy")
    assertThat(task?.outcome)
      .describedAs("compileGroovy task outcome")
      .isNotNull()
      .isEqualTo(TaskOutcome.SUCCESS)
  }

  @Test
  internal fun `can unit test code in src`() {
    projectDir.writeRelativeFile(fileName = "build.gradle") {
      groovyBuildScript()
    }
    projectDir.writeRelativeFile("src", "com", "mkobit", fileName = "MyLib.groovy") {
      """
package com.mkobit
class MyLib {
  int add(int a, int b) {
    return a + b
  }
}
"""
    }
    projectDir.writeRelativeFile("test", "unit", "groovy", "com", "mkobit", fileName = "MyLibTest.groovy") {
      """
package com.mkobit

import org.junit.Assert
import org.junit.Test

class MyLibTest {

  @Test
  void checkAddition() {
    def myLib = new MyLib()
    Assert.assertEquals(myLib.add(1, 2), 3)
  }
}
"""
    }

    val buildResult: BuildResult = GradleRunner.create()
      .withPluginClasspath()
      .withArguments("test", "-s")
      .withProjectDir(projectDir)
      .build()

    val task = buildResult.task(":test")
    assertThat(task).isNotNull()
    assertThat(task?.outcome)
      .describedAs("test task outcome")
      .withFailMessage("Build output: ${buildResult.output}")
      .isNotNull()
      .isEqualTo(TaskOutcome.SUCCESS)
  }

  @Test
  internal fun `can use @JenkinsRule in integration tests`() {
    projectDir.writeRelativeFile(fileName = "build.gradle") {
      groovyBuildScript()
    }

    projectDir.writeRelativeFile("test", "integration", "groovy", "com", "mkobit", fileName = "JenkinsRuleUsageTest.groovy") {
      """
package com.mkobit

import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.jvnet.hudson.test.JenkinsRule

class JenkinsRuleUsageTest {

  @Rule
  public JenkinsRule rule = new JenkinsRule()

  @Before
  void configureRule() {
    rule.timeout = 30
  }

  @Test
  void canUseJenkins() {
    rule.createFreeStyleProject()
    Assert.assertEquals(1, rule.jenkins.allItems.size())
  }
}
"""
    }

    val buildResult: BuildResult = GradleRunner.create()
      .withPluginClasspath()
      .withArguments("integrationTest", "-s", "-i")
      .withProjectDir(projectDir)
      .build()

    val task = buildResult.task(":integrationTest")
    assertThat(task?.outcome)
      .describedAs("integrationTest task outcome")
      .withFailMessage("Build output: ${buildResult.output}")
      .isNotNull()
      .isEqualTo(TaskOutcome.SUCCESS)
  }

  @NotImplementedYet
  @Test
  internal fun `integration test output for Jenkins Test Harness is in the build directory`() {
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
    assertThat(task).isNotNull()
    assertThat(task?.outcome)
      .describedAs("test task outcome")
      .withFailMessage("Build output: ${buildResult.output}")
      .isNotNull()
      .isEqualTo(TaskOutcome.SUCCESS)
  }

  // TODO: this should be tested but may or may not be needed. I have a feeling classloader errors
  // will happen in pipeline code if those classes are available.
  @NotImplementedYet
  @Test
  internal fun `cannot use classes from main source code in integration test`() {
  }

  @NotImplementedYet
  @Test
  internal fun `can set up pipeline library in an integration test`() {
    projectDir.writeRelativeFile(fileName = "build.gradle") {
      groovyBuildScript()
    }

    projectDir.writeRelativeFile("src", "com", "mkobit", fileName = "LibHelper.groovy") {
      """
package com.mkobit

class LibHelper {
  private script
  LibHelper(script) {
    this.script = script
  }

  void sayHelloTo(String name) {
    script.echo "LibHelper says hello to ${'$'}name!"
  }
}
"""
    }

    projectDir.writeRelativeFile("test", "integration", "groovy", "com", "mkobit", fileName = "JenkinsGlobalLibraryTest.groovy") {
      resourceText("com/mkobit/JenkinsGlobalLibraryTest.groovy")
    }

    val buildResult: BuildResult = GradleRunner.create()
      .withPluginClasspath()
      .withArguments("integrationTest", "-s", "-i")
      .withProjectDir(projectDir)
      .build()

    val task = buildResult.task(":integrationTest")
    assertThat(task?.outcome)
      .describedAs("integrationTest task outcome")
      .withFailMessage("Build output: ${buildResult.output}")
      .isNotNull()
      .isEqualTo(TaskOutcome.SUCCESS)
  }

  @NotImplementedYet
  @Test
  internal fun `can use declared plugin dependencies in integration test`() {
  }

  @NotImplementedYet
  @Test
  internal fun `@NonCPS can be used in source code`() {
  }

  @NotImplementedYet
  @Test
  internal fun `@Grab in library source is supported for trusted libraries`() {
  }

  @NotImplementedYet
  @Test
  internal fun `@Grab not supported for untrusted libraries`() {
  }

  @Test
  internal fun `Groovydoc JAR can be generated`() {
    projectDir.writeRelativeFile(fileName = "build.gradle") {
      groovyBuildScript()
    }
    projectDir.writeRelativeFile("src", "com", "mkobit", fileName = "MyLib.groovy") {
      """
package com.mkobit
class MyLib {
  int add(int a, int b) {
    return a + b
  }
}
"""
    }

    val buildResult: BuildResult = GradleRunner.create()
      .withPluginClasspath()
      .withArguments("groovydocJar")
      .withProjectDir(projectDir)
      .build()

    val task = buildResult.task(":groovydocJar")
    assertThat(task?.outcome)
      .describedAs("groovydocJar task outcome")
      .isNotNull()
      .isEqualTo(TaskOutcome.SUCCESS)
  }

  @Test
  internal fun `Groovy sources JAR can be generated`() {
    projectDir.writeRelativeFile(fileName = "build.gradle") {
      groovyBuildScript()
    }
    projectDir.writeRelativeFile("src", "com", "mkobit", fileName = "MyLib.groovy") {
      """
package com.mkobit
class MyLib {
  int add(int a, int b) {
    return a + b
  }
}
"""
    }

    val buildResult: BuildResult = GradleRunner.create()
      .withPluginClasspath()
      .withArguments("sourcesJar")
      .withProjectDir(projectDir)
      .build()

    val task = buildResult.task(":sourcesJar")
    assertThat(task?.outcome)
      .describedAs("groovydocJar task outcome")
      .isNotNull()
      .isEqualTo(TaskOutcome.SUCCESS)
  }

  private fun groovyBuildScript(): String = """
plugins {
  id 'com.mkobit.jenkins.pipelines.shared-library'
}

repositories {
  jcenter()
}

dependencies {
  testImplementation(group: 'junit', name: 'junit', version: '4.12')
}
"""

  private fun kotlinBuildScript(): String = """
plugins {
  id("com.mkobit.jenkins.pipelines.shared-library")
}

repositories {
  jcenter()
}

dependencies {
  testImplementation("junit", "junit", "4.12")
}
"""
}
