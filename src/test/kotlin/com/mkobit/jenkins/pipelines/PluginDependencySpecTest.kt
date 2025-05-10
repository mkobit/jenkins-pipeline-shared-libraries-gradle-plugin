package com.mkobit.jenkins.pipelines

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.exactly
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import testsupport.strikt.allOf
import testsupport.strikt.value
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PluginDependencySpecTest {
  private lateinit var pluginDependencySpec: PluginDependencySpec

  companion object {
    private const val INITIAL_WORKFLOW_API_PLUGIN_VERSION = "5.0"
    private const val INITIAL_WORKFLOW_BASIC_STEPS_PLUGIN_VERSION = "6.0"
    private const val INITIAL_WORKFLOW_CPS_PLUGIN_VERSION = "7.0"
    private const val INITIAL_WORKFLOW_DURABLE_TASK_STEP_PLUGIN_VERSION = "8.0"
    private const val INITIAL_WORKFLOW_CPS_GLOBAL_LIB_VERSION = "9.0"
    private const val INITIAL_WORKFLOW_JOB_PLUGIN_VERSION = "10.0"
    private const val INITIAL_WORKFLOW_MULTIBRANCH_PLUGIN_VERSION = "11.0"
    private const val INITIAL_WORKFLOW_SCM_STEP_PLUGIN_VERSION = "12.0"
    private const val INITIAL_WORKFLOW_STEP_API_PLUGIN_VERSION = "13.0"
    private const val INITIAL_WORKFLOW_SUPPORT_PLUGIN_VERSION = "14.0"
  }

  @BeforeEach
  internal fun setUp() {
    val project = ProjectBuilder.builder().build()
    pluginDependencySpec =
      PluginDependencySpec(
        project.initializedProperty(INITIAL_WORKFLOW_API_PLUGIN_VERSION),
        project.initializedProperty(INITIAL_WORKFLOW_BASIC_STEPS_PLUGIN_VERSION),
        project.initializedProperty(INITIAL_WORKFLOW_CPS_PLUGIN_VERSION),
        project.initializedProperty(INITIAL_WORKFLOW_DURABLE_TASK_STEP_PLUGIN_VERSION),
        project.initializedProperty(INITIAL_WORKFLOW_CPS_GLOBAL_LIB_VERSION),
        project.initializedProperty(INITIAL_WORKFLOW_JOB_PLUGIN_VERSION),
        project.initializedProperty(INITIAL_WORKFLOW_MULTIBRANCH_PLUGIN_VERSION),
        project.initializedProperty(INITIAL_WORKFLOW_SCM_STEP_PLUGIN_VERSION),
        project.initializedProperty(INITIAL_WORKFLOW_STEP_API_PLUGIN_VERSION),
        project.initializedProperty(INITIAL_WORKFLOW_SUPPORT_PLUGIN_VERSION),
        project.objects
      )
  }

  @Test
  internal fun `default versions can be retrieved`() {
    expect {
      that(pluginDependencySpec.workflowApiPluginVersion).value.isEqualTo(
        INITIAL_WORKFLOW_API_PLUGIN_VERSION
      )
      that(pluginDependencySpec.workflowBasicStepsPluginVersion).value.isEqualTo(
        INITIAL_WORKFLOW_BASIC_STEPS_PLUGIN_VERSION
      )
      that(pluginDependencySpec.workflowCpsPluginVersion).value.isEqualTo(
        INITIAL_WORKFLOW_CPS_PLUGIN_VERSION
      )
      that(pluginDependencySpec.workflowDurableTaskStepPluginVersion).value.isEqualTo(
        INITIAL_WORKFLOW_DURABLE_TASK_STEP_PLUGIN_VERSION
      )
      that(pluginDependencySpec.workflowCpsGlobalLibraryPluginVersion).value.isEqualTo(
        INITIAL_WORKFLOW_CPS_GLOBAL_LIB_VERSION
      )
      that(pluginDependencySpec.workflowJobPluginVersion).value.isEqualTo(
        INITIAL_WORKFLOW_JOB_PLUGIN_VERSION
      )
      that(pluginDependencySpec.workflowMultibranchPluginVersion).value.isEqualTo(
        INITIAL_WORKFLOW_MULTIBRANCH_PLUGIN_VERSION
      )
      that(pluginDependencySpec.workflowScmStepPluginVersion).value.isEqualTo(
        INITIAL_WORKFLOW_SCM_STEP_PLUGIN_VERSION
      )
      that(pluginDependencySpec.workflowStepApiPluginVersion).value.isEqualTo(
        INITIAL_WORKFLOW_STEP_API_PLUGIN_VERSION
      )
      that(pluginDependencySpec.workflowSupportPluginVersion).value.isEqualTo(
        INITIAL_WORKFLOW_SUPPORT_PLUGIN_VERSION
      )
    }
  }

  @Test
  internal fun `can set Workflow API Plugin version`() {
    pluginDependencySpec.workflowApiPluginVersion.set("newWorkflowApiVersion")

    expectThat(pluginDependencySpec.workflowApiPluginVersion)
      .value
      .isEqualTo("newWorkflowApiVersion")
  }

  @Test
  internal fun `can set Workflow Basic Steps Plugin version`() {
    pluginDependencySpec.workflowBasicStepsPluginVersion.set("newWorkflowBasicStepsPluginVersion")

    expectThat(pluginDependencySpec.workflowBasicStepsPluginVersion)
      .value
      .isEqualTo("newWorkflowBasicStepsPluginVersion")
  }

  @Test
  internal fun `can set Workflow CPS Plugin version`() {
    pluginDependencySpec.workflowCpsPluginVersion.set("newWorkflowCPSPluginVersion")

    expectThat(pluginDependencySpec.workflowCpsPluginVersion)
      .value
      .isEqualTo("newWorkflowCPSPluginVersion")
  }

  @Test
  internal fun `can set Workflow Durable Task Step Plugin Version`() {
    pluginDependencySpec.workflowDurableTaskStepPluginVersion.set("newWorkflowDurableTaskStepPluginVersion")

    expectThat(pluginDependencySpec.workflowDurableTaskStepPluginVersion)
      .value
      .isEqualTo("newWorkflowDurableTaskStepPluginVersion")
  }

  @Test
  internal fun `can set Workflow CPS Global Library Plugin version`() {
    pluginDependencySpec.workflowCpsGlobalLibraryPluginVersion.set("newWorkflowCpsGlobalLibraryPluginVersion")

    expectThat(pluginDependencySpec.workflowCpsGlobalLibraryPluginVersion)
      .value
      .isEqualTo("newWorkflowCpsGlobalLibraryPluginVersion")
  }

  @Test
  internal fun `can set Workflow Job Plugin version`() {
    pluginDependencySpec.workflowJobPluginVersion.set("newWorkflowJobPluginVersion")

    expectThat(pluginDependencySpec.workflowJobPluginVersion)
      .value
      .isEqualTo("newWorkflowJobPluginVersion")
  }

  @Test
  internal fun `can set Workflow Multibranch Plugin version`() {
    pluginDependencySpec.workflowMultibranchPluginVersion.set("newWorkflowMultibranchPluginVersion")

    expectThat(pluginDependencySpec.workflowMultibranchPluginVersion)
      .value
      .isEqualTo("newWorkflowMultibranchPluginVersion")
  }

  @Test
  internal fun `can set Workflow Step API version`() {
    pluginDependencySpec.workflowStepApiPluginVersion.set("newWorkflowStepApiPluginVersion")

    expectThat(pluginDependencySpec.workflowStepApiPluginVersion)
      .value
      .isEqualTo("newWorkflowStepApiPluginVersion")
  }

  @Test
  internal fun `can set Workflow Support version`() {
    pluginDependencySpec.workflowSupportPluginVersion.set("newWorkflowSupportPluginVersion")

    expectThat(pluginDependencySpec.workflowSupportPluginVersion)
      .value
      .isEqualTo("newWorkflowSupportPluginVersion")
  }

  @Test
  internal fun `adding multiple plugins`() {
    val initialSize = pluginDependencySpec.pluginDependencies().get().size
    pluginDependencySpec.dependency("com.mkobit", "mkobit-a", "1.0")
    expectThat(pluginDependencySpec.pluginDependencies()).get { get() }.hasSize(initialSize + 1)
    pluginDependencySpec.dependency("com.mkobit", "mkobit-b", "2.0")
    expectThat(pluginDependencySpec.pluginDependencies()).get { get() }.hasSize(initialSize + 2)
    pluginDependencySpec.dependency("com.mkobit", "mkobit-c", "3.0")
    expectThat(pluginDependencySpec.pluginDependencies()).get { get() }.hasSize(initialSize + 3)
  }

  @ParameterizedTest(name = "{0} with artifact Id {1}")
  @MethodSource("requiredPlugins")
  internal fun `plugin dependency includes`(
    @Suppress("UNUSED") pluginName: String,
    artifactId: String,
    version: String
  ) {
    val pluginDependencies = pluginDependencySpec.pluginDependencies()

    expectThat(pluginDependencies)
      .get { get() }
      .exactly(1) {
        allOf {
          get { name }.isEqualTo(artifactId)
          get { version }.isEqualTo(version)
        }
      }
  }

  @Suppress("UNUSED")
  private fun requiredPlugins(): Stream<Arguments> =
    Stream.of(
      // TODO: should we include scm-api?
      // Arguments.of("SCM API Plugin", "scm-api"),
      Arguments.of("Workflow API Plugin", "workflow-api", INITIAL_WORKFLOW_API_PLUGIN_VERSION),
      Arguments.of("Workflow Basic Steps Plugin", "workflow-basic-steps", INITIAL_WORKFLOW_BASIC_STEPS_PLUGIN_VERSION),
      Arguments.of("Workflow CPS Plugin", "workflow-cps", INITIAL_WORKFLOW_CPS_PLUGIN_VERSION),
      Arguments.of("Workflow Durable Task Step Plugin", "workflow-durable-task-step", INITIAL_WORKFLOW_DURABLE_TASK_STEP_PLUGIN_VERSION),
      Arguments.of("Workflow Job Plugin", "workflow-job", INITIAL_WORKFLOW_JOB_PLUGIN_VERSION),
      Arguments.of("Workflow Multibranch Plugin", "workflow-multibranch", INITIAL_WORKFLOW_MULTIBRANCH_PLUGIN_VERSION),
      Arguments.of("Workflow SCM Step Plugin", "workflow-scm-step", INITIAL_WORKFLOW_SCM_STEP_PLUGIN_VERSION),
      Arguments.of("Workflow Step API Plugin", "workflow-step-api", INITIAL_WORKFLOW_STEP_API_PLUGIN_VERSION),
      Arguments.of("Workflow Support Plugin", "workflow-support", INITIAL_WORKFLOW_SUPPORT_PLUGIN_VERSION)
    )
}
