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
    private val INITIAL_PIPELINE_UNIT_VERSION = "3.0"
    private val INITIAL_TEST_HARNESS_VERSION = "4.0"
    private val INITIAL_WORKFLOW_API_PLUGIN_VERSION = "5.0"
    private val INITIAL_WORKFLOW_BASIC_STEPS_PLUGIN_VERSION = "6.0"
    private val INITIAL_WORKFLOW_CPS_PLUGIN_VERSION = "7.0"
    private val INITIAL_WORKFLOW_DURABLE_TASK_STEP_PLUGIN_VERSION = "8.0"
    private val INITIAL_GLOBAL_LIB_VERSION = "9.0"
    private val INITIAL_WORKFLOW_JOB_PLUGIN_VERSION = "10.0"
    private val INITIAL_WORKFLOW_MULTIBRANCH_PLUGIN_VERSION = "11.0"
    private val INITIAL_WORKFLOW_SCM_STEP_PLUGIN_VERSION = "12.0"
    private val INITIAL_WORKFLOW_STEP_API_PLUGIN_VERSION = "13.0"
    private val INITIAL_WORKFLOW_SUPPORT_PLUGIN_VERSION = "14.0"
  }

  @BeforeEach
  internal fun setUp() {
    val project = ProjectBuilder.builder().build()
    sharedLibraryExtension = SharedLibraryExtension(
      project.initializedProperty(INITIAL_GROOVY_VERSION),
      project.initializedProperty(INITIAL_CORE_VERSION),
      project.initializedProperty(INITIAL_PIPELINE_UNIT_VERSION),
      project.initializedProperty(INITIAL_TEST_HARNESS_VERSION),
      project.initializedProperty(INITIAL_WORKFLOW_API_PLUGIN_VERSION),
      project.initializedProperty(INITIAL_WORKFLOW_BASIC_STEPS_PLUGIN_VERSION),
      project.initializedProperty(INITIAL_WORKFLOW_CPS_PLUGIN_VERSION),
      project.initializedProperty(INITIAL_WORKFLOW_DURABLE_TASK_STEP_PLUGIN_VERSION),
      project.initializedProperty(INITIAL_GLOBAL_LIB_VERSION),
      project.initializedProperty(INITIAL_WORKFLOW_JOB_PLUGIN_VERSION),
      project.initializedProperty(INITIAL_WORKFLOW_MULTIBRANCH_PLUGIN_VERSION),
      project.initializedProperty(INITIAL_WORKFLOW_SCM_STEP_PLUGIN_VERSION),
      project.initializedProperty(INITIAL_WORKFLOW_STEP_API_PLUGIN_VERSION),
      project.initializedProperty(INITIAL_WORKFLOW_SUPPORT_PLUGIN_VERSION)
    )
  }

  @Test
  internal fun `default versions can be retrieved`() {
    softlyAssert {
      assertThat(sharedLibraryExtension.groovyVersion).isEqualTo(INITIAL_GROOVY_VERSION)
      assertThat(sharedLibraryExtension.coreVersion).isEqualTo(INITIAL_CORE_VERSION)
      assertThat(sharedLibraryExtension.testHarnessVersion).isEqualTo(
        INITIAL_TEST_HARNESS_VERSION)
      assertThat(sharedLibraryExtension.pipelineTestUnitVersion).isEqualTo(
        INITIAL_PIPELINE_UNIT_VERSION)
      assertThat(sharedLibraryExtension.workflowApiPluginVersion).isEqualTo(
        INITIAL_WORKFLOW_API_PLUGIN_VERSION)
      assertThat(sharedLibraryExtension.workflowBasicStepsPluginVersion).isEqualTo(
        INITIAL_WORKFLOW_BASIC_STEPS_PLUGIN_VERSION)
      assertThat(sharedLibraryExtension.workflowCpsPluginVersion).isEqualTo(
        INITIAL_WORKFLOW_CPS_PLUGIN_VERSION)
      assertThat(sharedLibraryExtension.workflowDurableTaskStepPluginVersion).isEqualTo(
        INITIAL_WORKFLOW_DURABLE_TASK_STEP_PLUGIN_VERSION)
      assertThat(sharedLibraryExtension.workflowCpsGlobalLibraryPluginVersion).isEqualTo(
        INITIAL_GLOBAL_LIB_VERSION)
      assertThat(sharedLibraryExtension.workflowJobPluginVersion).isEqualTo(INITIAL_WORKFLOW_JOB_PLUGIN_VERSION)
      assertThat(sharedLibraryExtension.workflowMultibranchPluginVersion).isEqualTo(INITIAL_WORKFLOW_MULTIBRANCH_PLUGIN_VERSION)
      assertThat(sharedLibraryExtension.workflowScmStepPluginVersion).isEqualTo(INITIAL_WORKFLOW_SCM_STEP_PLUGIN_VERSION)
      assertThat(sharedLibraryExtension.workflowStepApiPluginVersion).isEqualTo(INITIAL_WORKFLOW_STEP_API_PLUGIN_VERSION)
      assertThat(sharedLibraryExtension.workflowSupportPluginVersion).isEqualTo(INITIAL_WORKFLOW_SUPPORT_PLUGIN_VERSION)
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
  internal fun `can set PipelineTestUnit version`() {
    sharedLibraryExtension.pipelineTestUnitVersion = "newPipelineTestUnitVersion"

    assertThat(sharedLibraryExtension.pipelineTestUnitVersion).isEqualTo("newPipelineTestUnitVersion")
  }

  @Test
  internal fun `can set Jenkins Test Harness version`() {
    sharedLibraryExtension.testHarnessVersion = "newTestHarnessVersion"

    assertThat(sharedLibraryExtension.testHarnessVersion).isEqualTo("newTestHarnessVersion")
  }

  @Test
  internal fun `can set Workflow API Plugin version`() {
    sharedLibraryExtension.workflowApiPluginVersion = "newWorkflowApiVersion"

    assertThat(sharedLibraryExtension.workflowApiPluginVersion).isEqualTo("newWorkflowApiVersion")
  }

  @Test
  internal fun `can set Workflow Basic Steps Plugin version`() {
    sharedLibraryExtension.workflowBasicStepsPluginVersion = "newWorkflowBasicStepsPluginVersion"

    assertThat(sharedLibraryExtension.workflowBasicStepsPluginVersion).isEqualTo("newWorkflowBasicStepsPluginVersion")
  }

  @Test
  internal fun `can set Workflow CPS Plugin version`() {
    sharedLibraryExtension.workflowCpsPluginVersion = "newWorkflowCPSPluginVersion"

    assertThat(sharedLibraryExtension.workflowCpsPluginVersion).isEqualTo("newWorkflowCPSPluginVersion")
  }

  @Test
  internal fun `can set Workflow Durable Task Step Plugin Version`() {
    sharedLibraryExtension.workflowDurableTaskStepPluginVersion = "newWorkflowDurableTaskStepPluginVersion"

    assertThat(sharedLibraryExtension.workflowDurableTaskStepPluginVersion).isEqualTo("newWorkflowDurableTaskStepPluginVersion")
  }

  @Test
  internal fun `can set Workflow CPS Global Library Plugin version`() {
    sharedLibraryExtension.workflowCpsGlobalLibraryPluginVersion = "newWorkflowCpsGlobalLibraryPluginVersion"

    assertThat(sharedLibraryExtension.workflowCpsGlobalLibraryPluginVersion).isEqualTo("newWorkflowCpsGlobalLibraryPluginVersion")
  }

  @Test
  internal fun `can set Workflow Job Plugin version`() {
    sharedLibraryExtension.workflowJobPluginVersion = "newWorkflowJobPluginVersion"

    assertThat(sharedLibraryExtension.workflowJobPluginVersion).isEqualTo("newWorkflowJobPluginVersion")
  }

  @Test
  internal fun `can set Workflow Multibranch Plugin version`() {
    sharedLibraryExtension.workflowMultibranchPluginVersion = "newWorkflowMultibranchPluginVersion"

    assertThat(sharedLibraryExtension.workflowMultibranchPluginVersion).isEqualTo("newWorkflowMultibranchPluginVersion")
  }

  @Test
  internal fun `can set Workflow Step API version`() {
    sharedLibraryExtension.workflowStepApiPluginVersion = "newWorkflowStepApiPluginVersion"

    assertThat(sharedLibraryExtension.workflowStepApiPluginVersion).isEqualTo("newWorkflowStepApiPluginVersion")
  }

  @Test
  internal fun `can set Workflow Support version`() {
    sharedLibraryExtension.workflowSupportPluginVersion = "newWorkflowSupportPluginVersion"

    assertThat(sharedLibraryExtension.workflowSupportPluginVersion).isEqualTo("newWorkflowSupportPluginVersion")
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
      Arguments.of("SCM API Plugin"),
      Arguments.of("Git Plugin"),
      Arguments.of("Workflow API Plugin"),
      Arguments.of("Workflow Basic Steps Plugin"),
      Arguments.of("Workflow CPS Plugin"),
      Arguments.of("Workflow Durable Task Step Plugin"),
      Arguments.of("Workflow Job Plugin"),
      Arguments.of("Workflow Multibranch Plugin"),
      Arguments.of("Workflow SCM Step Plugin"),
      Arguments.of("Workflow Step API Plugin"),
      Arguments.of("Workflow Support Plugin")
    )
  }

  private fun softlyAssert(assertions: SoftAssertions.() -> Unit) {
    val softAssertions = SoftAssertions()
    softAssertions.assertions()
    softAssertions.assertAll()
  }
}
