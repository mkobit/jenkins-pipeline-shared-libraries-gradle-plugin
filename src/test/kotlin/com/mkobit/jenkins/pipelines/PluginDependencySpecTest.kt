package com.mkobit.jenkins.pipelines

import org.assertj.core.api.Assertions.allOf
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Condition
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import testsupport.condition
import testsupport.softlyAssert
import java.util.function.Predicate
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PluginDependencySpecTest {

  private lateinit var pluginDependencySpec: PluginDependencySpec

  companion object {
    private val INITIAL_GIT_PLUGIN_VERSION_VERSION = "4.0"
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
    pluginDependencySpec = PluginDependencySpec(
      project.initializedProperty(INITIAL_GIT_PLUGIN_VERSION_VERSION),
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
      assertThat(pluginDependencySpec.gitPluginVersion).isEqualTo(
        INITIAL_GIT_PLUGIN_VERSION_VERSION
      )
      assertThat(pluginDependencySpec.workflowApiPluginVersion).isEqualTo(
        INITIAL_WORKFLOW_API_PLUGIN_VERSION
      )
      assertThat(pluginDependencySpec.workflowBasicStepsPluginVersion).isEqualTo(
        INITIAL_WORKFLOW_BASIC_STEPS_PLUGIN_VERSION
      )
      assertThat(pluginDependencySpec.workflowCpsPluginVersion).isEqualTo(
        INITIAL_WORKFLOW_CPS_PLUGIN_VERSION
      )
      assertThat(pluginDependencySpec.workflowDurableTaskStepPluginVersion).isEqualTo(
        INITIAL_WORKFLOW_DURABLE_TASK_STEP_PLUGIN_VERSION
      )
      assertThat(pluginDependencySpec.workflowCpsGlobalLibraryPluginVersion).isEqualTo(
        INITIAL_GLOBAL_LIB_VERSION
      )
      assertThat(pluginDependencySpec.workflowJobPluginVersion).isEqualTo(
        INITIAL_WORKFLOW_JOB_PLUGIN_VERSION
      )
      assertThat(pluginDependencySpec.workflowMultibranchPluginVersion).isEqualTo(
        INITIAL_WORKFLOW_MULTIBRANCH_PLUGIN_VERSION
      )
      assertThat(pluginDependencySpec.workflowScmStepPluginVersion).isEqualTo(
        INITIAL_WORKFLOW_SCM_STEP_PLUGIN_VERSION
      )
      assertThat(pluginDependencySpec.workflowStepApiPluginVersion).isEqualTo(
        INITIAL_WORKFLOW_STEP_API_PLUGIN_VERSION
      )
      assertThat(pluginDependencySpec.workflowSupportPluginVersion).isEqualTo(
        INITIAL_WORKFLOW_SUPPORT_PLUGIN_VERSION
      )
    }
  }

  @Test
  internal fun `can set Git Plugin version`() {
    pluginDependencySpec.gitPluginVersion = "newGitVersion"

    assertThat(pluginDependencySpec.gitPluginVersion).isEqualTo("newGitVersion")
  }

  @Test
  internal fun `can set Workflow API Plugin version`() {
    pluginDependencySpec.workflowApiPluginVersion = "newWorkflowApiVersion"

    assertThat(pluginDependencySpec.workflowApiPluginVersion).isEqualTo("newWorkflowApiVersion")
  }

  @Test
  internal fun `can set Workflow Basic Steps Plugin version`() {
    pluginDependencySpec.workflowBasicStepsPluginVersion = "newWorkflowBasicStepsPluginVersion"

    assertThat(pluginDependencySpec.workflowBasicStepsPluginVersion).isEqualTo("newWorkflowBasicStepsPluginVersion")
  }

  @Test
  internal fun `can set Workflow CPS Plugin version`() {
    pluginDependencySpec.workflowCpsPluginVersion = "newWorkflowCPSPluginVersion"

    assertThat(pluginDependencySpec.workflowCpsPluginVersion).isEqualTo("newWorkflowCPSPluginVersion")
  }

  @Test
  internal fun `can set Workflow Durable Task Step Plugin Version`() {
    pluginDependencySpec.workflowDurableTaskStepPluginVersion = "newWorkflowDurableTaskStepPluginVersion"

    assertThat(pluginDependencySpec.workflowDurableTaskStepPluginVersion).isEqualTo("newWorkflowDurableTaskStepPluginVersion")
  }

  @Test
  internal fun `can set Workflow CPS Global Library Plugin version`() {
    pluginDependencySpec.workflowCpsGlobalLibraryPluginVersion = "newWorkflowCpsGlobalLibraryPluginVersion"

    assertThat(pluginDependencySpec.workflowCpsGlobalLibraryPluginVersion).isEqualTo("newWorkflowCpsGlobalLibraryPluginVersion")
  }

  @Test
  internal fun `can set Workflow Job Plugin version`() {
    pluginDependencySpec.workflowJobPluginVersion = "newWorkflowJobPluginVersion"

    assertThat(pluginDependencySpec.workflowJobPluginVersion).isEqualTo("newWorkflowJobPluginVersion")
  }

  @Test
  internal fun `can set Workflow Multibranch Plugin version`() {
    pluginDependencySpec.workflowMultibranchPluginVersion = "newWorkflowMultibranchPluginVersion"

    assertThat(pluginDependencySpec.workflowMultibranchPluginVersion).isEqualTo("newWorkflowMultibranchPluginVersion")
  }

  @Test
  internal fun `can set Workflow Step API version`() {
    pluginDependencySpec.workflowStepApiPluginVersion = "newWorkflowStepApiPluginVersion"

    assertThat(pluginDependencySpec.workflowStepApiPluginVersion).isEqualTo("newWorkflowStepApiPluginVersion")
  }

  @Test
  internal fun `can set Workflow Support version`() {
    pluginDependencySpec.workflowSupportPluginVersion = "newWorkflowSupportPluginVersion"

    assertThat(pluginDependencySpec.workflowSupportPluginVersion).isEqualTo("newWorkflowSupportPluginVersion")
  }

  @ParameterizedTest(name = "group {1}")
  @MethodSource("pluginCreators")
  internal fun `create plugin for`(
    addPlugin: PluginDependencySpec.(String, String) -> Unit,
    expectedGroup: String
  ) {
    val expectedName = "name"
    val expectedVersion = "1.0"

    pluginDependencySpec.addPlugin(expectedName, expectedVersion)

    val group: Condition<PluginDependency> = condition(expectedGroup) { group == expectedGroup }
    val name: Condition<PluginDependency> = condition(expectedName) { name == expectedName }
    val version: Condition<PluginDependency> = condition(expectedVersion) { version == expectedVersion }
    assertThat(pluginDependencySpec.pluginDependencies()).haveExactly(1, allOf(group, name, version))
  }

  @Suppress("UNUSED")
  private fun pluginCreators(): Stream<Arguments> {
    fun argsOf(addPlugin: PluginDependencySpec.(String, String) -> Unit,
               expectedGroup: String): Arguments = Arguments.of(addPlugin, expectedGroup)

    return Stream.of(
      argsOf({ name, version -> blueocean(name, version) }, "io.jenkins.blueocean"),
      argsOf({ name, version -> jvnet(name, version) }, "org.jvnet.hudson.plugins"),
      argsOf({ name, version -> jenkinsCi(name, version) }, "org.jenkins-ci.plugins"),
      argsOf({ name, version -> workflow(name, version) }, "org.jenkins-ci.plugins.workflow"),
      argsOf({ name, version -> cloudbees(name, version) }, "com.cloudbees.jenkins.plugins"),
      argsOf({ name, version -> dependency("com.mkobit.plugin", name, version) }, "com.mkobit.plugin")
    )
  }

  @Test
  internal fun `adding multiple plugins`() {
    val initialSize = pluginDependencySpec.pluginDependencies().size
    pluginDependencySpec.cloudbees("cloudbees", "1.0")
    assertThat(pluginDependencySpec.pluginDependencies()).hasSize(initialSize + 1)
    pluginDependencySpec.workflow("workflow", "2.0")
    assertThat(pluginDependencySpec.pluginDependencies()).hasSize(initialSize + 2)
    pluginDependencySpec.jvnet("jvent", "3.0")
    assertThat(pluginDependencySpec.pluginDependencies()).hasSize(initialSize + 3)
    pluginDependencySpec.jenkinsCi("jenkinsCi", "4.0")
    assertThat(pluginDependencySpec.pluginDependencies()).hasSize(initialSize + 4)
    pluginDependencySpec.jenkinsCi("blueocean", "5.0")
    assertThat(pluginDependencySpec.pluginDependencies()).hasSize(initialSize + 5)
    pluginDependencySpec.dependency("com.mkobit", "mkobit-plugin", "6.0")
    assertThat(pluginDependencySpec.pluginDependencies()).hasSize(initialSize + 6)
  }

  @ParameterizedTest(name = "{0} with artifact Id {1}")
  @MethodSource("requiredPlugins")
  internal fun `plugin dependency includes`(pluginName: String, artifactId: String, version: String) {
    val pluginDependencies = pluginDependencySpec.pluginDependencies()

    val artifactCondition = Condition<PluginDependency>(Predicate {
      it.name == artifactId
    }, artifactId)
    val versionCondition = Condition<PluginDependency>(Predicate {
      it.version == version
    }, version)

    assertThat(pluginDependencies).haveExactly(1, allOf(artifactCondition, versionCondition))
  }

  @Suppress("UNUSED")
  private fun requiredPlugins(): Stream<Arguments> {
    return Stream.of(
      // TODO: should we include scm-api?
//      Arguments.of("SCM API Plugin", "scm-api"),
      Arguments.of("Git Plugin", "git", INITIAL_GIT_PLUGIN_VERSION_VERSION),
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
}
