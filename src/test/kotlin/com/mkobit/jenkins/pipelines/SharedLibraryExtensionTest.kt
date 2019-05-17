package com.mkobit.jenkins.pipelines

import com.nhaarman.mockitokotlin2.mock
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import testsupport.strikt.value

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SharedLibraryExtensionTest {
  private lateinit var sharedLibraryExtension: SharedLibraryExtension

  companion object {
    private const val INITIAL_CORE_VERSION = "2.0"
    private const val INITIAL_PIPELINE_UNIT_VERSION = "3.0"
    private const val INITIAL_TEST_HARNESS_VERSION = "4.0"
  }

  @BeforeEach
  internal fun setUp() {
    val project = ProjectBuilder.builder().build()
    sharedLibraryExtension = SharedLibraryExtension(
      project.initializedProperty(INITIAL_CORE_VERSION),
      project.initializedProperty(INITIAL_PIPELINE_UNIT_VERSION),
      project.initializedProperty(INITIAL_TEST_HARNESS_VERSION),
      mock()
    )
  }

  @Test
  internal fun `default versions can be retrieved`() {
    expect {
      that(sharedLibraryExtension.coreVersion)
        .value
        .isEqualTo(INITIAL_CORE_VERSION)
      that(sharedLibraryExtension.testHarnessVersion)
        .value
        .isEqualTo(INITIAL_TEST_HARNESS_VERSION)
      that(sharedLibraryExtension.pipelineTestUnitVersion)
        .value
        .isEqualTo(INITIAL_PIPELINE_UNIT_VERSION)
    }
  }

  @Test
  internal fun `can set Jenkins core version`() {
    sharedLibraryExtension.coreVersion.set("newCoreVersion")

    expectThat(sharedLibraryExtension.coreVersion)
      .value
      .isEqualTo("newCoreVersion")
  }

  @Test
  internal fun `can set PipelineTestUnit version`() {
    sharedLibraryExtension.pipelineTestUnitVersion.set("newPipelineTestUnitVersion")

    expectThat(sharedLibraryExtension.pipelineTestUnitVersion)
      .value
      .isEqualTo("newPipelineTestUnitVersion")
  }

  @Test
  internal fun `can set Jenkins Test Harness version`() {
    sharedLibraryExtension.testHarnessVersion.set("newTestHarnessVersion")

    expectThat(sharedLibraryExtension.testHarnessVersion).value.isEqualTo("newTestHarnessVersion")
  }
}
