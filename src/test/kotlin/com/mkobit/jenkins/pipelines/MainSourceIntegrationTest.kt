package com.mkobit.jenkins.pipelines

import org.assertj.core.api.Assertions
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import testsupport.NotImplementedYet
import testsupport.build
import testsupport.writeRelativeFile
import java.io.File

@Tag("integration")
class MainSourceIntegrationTest {
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

    val buildResult: BuildResult = build(projectDir, "compileGroovy")

    val task = buildResult.task(":compileGroovy")
    Assertions.assertThat(task?.outcome)
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

    val buildResult: BuildResult = build(projectDir, "test", "-s")

    val task = buildResult.task(":test")
    Assertions.assertThat(task).isNotNull()
    Assertions.assertThat(task?.outcome)
      .describedAs("test task outcome")
      .withFailMessage("Build output: ${buildResult.output}")
      .isNotNull()
      .isEqualTo(TaskOutcome.SUCCESS)
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

    val buildResult: BuildResult = build(projectDir, "groovydocJar")

    val task = buildResult.task(":groovydocJar")
    Assertions.assertThat(task?.outcome)
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

    val buildResult: BuildResult = build(projectDir, "sourcesJar")

    val task = buildResult.task(":sourcesJar")
    Assertions.assertThat(task?.outcome)
      .describedAs("groovydocJar task outcome")
      .isNotNull()
      .isEqualTo(TaskOutcome.SUCCESS)
  }
}
