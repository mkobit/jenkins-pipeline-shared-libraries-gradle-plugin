package com.mkobit.jenkins.pipelines

import io.kotest.matchers.shouldBe
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SharedLibraryExtensionTest {
  private lateinit var sharedLibraryExtension: SharedLibraryExtension

  companion object {
    private const val INITIAL_PIPELINE_UNIT_VERSION = "3.0"
    private const val INITIAL_TEST_HARNESS_VERSION = "4.0"
  }

  @BeforeEach
  internal fun setUp() {
    val project = ProjectBuilder.builder().build()
    sharedLibraryExtension =
      SharedLibraryExtension(
        project.initializedProperty(INITIAL_PIPELINE_UNIT_VERSION),
        project.initializedProperty(INITIAL_TEST_HARNESS_VERSION),
      )
  }

  @Test
  internal fun `default versions can be retrieved`() {
    sharedLibraryExtension.pipelineTestUnitVersion.get() shouldBe INITIAL_PIPELINE_UNIT_VERSION
    sharedLibraryExtension.testHarnessVersion.get() shouldBe INITIAL_TEST_HARNESS_VERSION
  }

  @Test
  internal fun `can set PipelineTestUnit version`() {
    sharedLibraryExtension.pipelineTestUnitVersion.set("newPipelineTestUnitVersion")
    sharedLibraryExtension.pipelineTestUnitVersion.get() shouldBe "newPipelineTestUnitVersion"
  }

  @Test
  internal fun `can set Jenkins Test Harness version`() {
    sharedLibraryExtension.testHarnessVersion.set("newTestHarnessVersion")
    sharedLibraryExtension.testHarnessVersion.get() shouldBe "newTestHarnessVersion"
  }
}
