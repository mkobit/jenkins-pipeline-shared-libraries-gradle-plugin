package com.mkobit.jenkins.pipelines

import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

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
    softlyAssert {
      assertThat(sharedLibraryExtension.coreVersion.get()).isEqualTo(INITIAL_CORE_VERSION)
      assertThat(sharedLibraryExtension.testHarnessVersion.get()).isEqualTo(
        INITIAL_TEST_HARNESS_VERSION)
      assertThat(sharedLibraryExtension.pipelineTestUnitVersion.get()).isEqualTo(
        INITIAL_PIPELINE_UNIT_VERSION
      )
    }
  }

  @Test
  internal fun `can set Jenkins core version`() {
    sharedLibraryExtension.coreVersion.set("newCoreVersion")

    assertThat(sharedLibraryExtension.coreVersion.get()).isEqualTo("newCoreVersion")
  }

  @Test
  internal fun `can set PipelineTestUnit version`() {
    sharedLibraryExtension.pipelineTestUnitVersion.set("newPipelineTestUnitVersion")

    assertThat(sharedLibraryExtension.pipelineTestUnitVersion.get()).isEqualTo("newPipelineTestUnitVersion")
  }

  @Test
  internal fun `can set Jenkins Test Harness version`() {
    sharedLibraryExtension.testHarnessVersion.set("newTestHarnessVersion")

    assertThat(sharedLibraryExtension.testHarnessVersion.get()).isEqualTo("newTestHarnessVersion")
  }

  private fun softlyAssert(assertions: SoftAssertions.() -> Unit) {
    val softAssertions = SoftAssertions()
    softAssertions.assertions()
    softAssertions.assertAll()
  }
}
