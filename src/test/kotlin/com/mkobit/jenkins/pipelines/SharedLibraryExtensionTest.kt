package com.mkobit.jenkins.pipelines

import org.assertj.core.api.SoftAssertions
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import testsupport.NotImplementedYet

internal class SharedLibraryExtensionTest {
  private lateinit var sharedLibraryExtension: SharedLibraryExtension

  companion object {
    private val INITIAL_GROOVY_VERSION = "1.0"
    private val INITIAL_CORE_VERSION = "2.0"
    private val INITIAL_GLOBAL_LIB_VERSION = "3.0"
    private val INITIAL_TEST_HARNESS_VERSION = "4.0"
  }

  @BeforeEach
  internal fun setUp() {
    val project = ProjectBuilder.builder().build()
    sharedLibraryExtension = SharedLibraryExtension(
      project.initializedProperty(INITIAL_GROOVY_VERSION),
      project.initializedProperty(INITIAL_CORE_VERSION),
      project.initializedProperty(INITIAL_GLOBAL_LIB_VERSION),
      project.initializedProperty(INITIAL_TEST_HARNESS_VERSION)
    )
  }

  @Test
  internal fun `default versions can be retrieved`() {
    val soft = SoftAssertions()
    soft.assertThat(sharedLibraryExtension.groovyVersion).isEqualTo(INITIAL_GROOVY_VERSION)
    soft.assertThat(sharedLibraryExtension.coreVersion).isEqualTo(INITIAL_CORE_VERSION)
    soft.assertThat(sharedLibraryExtension.globalLibPluginVersion).isEqualTo(
      INITIAL_GLOBAL_LIB_VERSION)
    soft.assertThat(sharedLibraryExtension.testHarnessVersion).isEqualTo(
      INITIAL_TEST_HARNESS_VERSION)
    soft.assertAll()
  }

  @NotImplementedYet
  @Test
  internal fun `can set Groovy version`() {
  }

  @NotImplementedYet
  @Test
  internal fun `can set Jenkins core version`() {
  }

  @NotImplementedYet
  @Test
  internal fun `can set Global Library Plugin version`() {
  }

  @NotImplementedYet
  @Test
  internal fun `can set Jenkins Test Harness version`() {
  }
}
