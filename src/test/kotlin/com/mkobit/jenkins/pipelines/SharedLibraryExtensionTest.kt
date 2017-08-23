package com.mkobit.jenkins.pipelines

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import testsupport.NotImplementedYet
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SharedLibraryExtensionTest {
  private lateinit var sharedLibraryExtension: SharedLibraryExtension

  companion object {
    private val INITIAL_GROOVY_VERSION = "1.0"
    private val INITIAL_CORE_VERSION = "2.0"
    private val INITIAL_GLOBAL_LIB_VERSION = "3.0"
    private val INITIAL_TEST_HARNESS_VERSION = "4.0"
    private val INITIAL_PIPELINE_UNIT_VERSION = "5.0"
  }

  @BeforeEach
  internal fun setUp() {
    val project = ProjectBuilder.builder().build()
    sharedLibraryExtension = SharedLibraryExtension(
      project.initializedProperty(INITIAL_GROOVY_VERSION),
      project.initializedProperty(INITIAL_CORE_VERSION),
      project.initializedProperty(INITIAL_GLOBAL_LIB_VERSION),
      project.initializedProperty(INITIAL_TEST_HARNESS_VERSION),
      project.initializedProperty(INITIAL_PIPELINE_UNIT_VERSION)
    )
  }

  @Test
  internal fun `default versions can be retrieved`() {
    softlyAssert {
      assertThat(sharedLibraryExtension.groovyVersion).isEqualTo(INITIAL_GROOVY_VERSION)
      assertThat(sharedLibraryExtension.coreVersion).isEqualTo(INITIAL_CORE_VERSION)
      assertThat(sharedLibraryExtension.globalLibPluginVersion).isEqualTo(
        INITIAL_GLOBAL_LIB_VERSION)
      assertThat(sharedLibraryExtension.testHarnessVersion).isEqualTo(
        INITIAL_TEST_HARNESS_VERSION)
      assertThat(sharedLibraryExtension.pipelineTestUnitVersion).isEqualTo(
        INITIAL_PIPELINE_UNIT_VERSION)
    }
  }

  @Test
  internal fun `can set Groovy version`() {
    sharedLibraryExtension.groovyVersion = "newGroovyVersion"

    assertThat(sharedLibraryExtension.groovyVersion).isEqualTo("newGroovyVersion")
  }

  @Test
  internal fun `can set Jenkins core version`() {
    sharedLibraryExtension.coreVersion = "newCoreVersion"

    assertThat(sharedLibraryExtension.coreVersion).isEqualTo("newCoreVersion")
  }

  @Test
  internal fun `can set Global Library Plugin version`() {
    sharedLibraryExtension.globalLibPluginVersion = "newGlobalLibraryVersion"

    assertThat(sharedLibraryExtension.globalLibPluginVersion).isEqualTo("newGlobalLibraryVersion")
  }

  @Test
  internal fun `can set Jenkins Test Harness version`() {
    sharedLibraryExtension.testHarnessVersion = "newTestHarnessVersion"

    assertThat(sharedLibraryExtension.testHarnessVersion).isEqualTo("newTestHarnessVersion")
  }

  @Test
  internal fun `can set PipelineTestUnit version`() {
    sharedLibraryExtension.pipelineTestUnitVersion = "newPipelineTestUnitVersion"

    assertThat(sharedLibraryExtension.pipelineTestUnitVersion).isEqualTo("newPipelineTestUnitVersion")
  }

  @NotImplementedYet
  @Test
  internal fun `can set a URL to a target Jenkins instance`() {
  }

  @NotImplementedYet
  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("requiredPlugins")
  internal fun `plugin dependency includes`(pluginName: String) {
  }

  fun requiredPlugins(): Stream<Arguments> {
    return Stream.of(
      Arguments.of("Global Shared Library Plugin"),
      Arguments.of("SCM API Plugin"),
      Arguments.of("Git Plugin"),
      Arguments.of("Workflow API Plugin"),
      Arguments.of("Workflow Job Plugin")
    )
  }

  private fun softlyAssert(assertions: SoftAssertions.() -> Unit) {
    val softAssertions = SoftAssertions()
    softAssertions.assertions()
    softAssertions.assertAll()
  }
}
