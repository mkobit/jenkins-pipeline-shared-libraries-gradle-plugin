package com.mkobit.jenkins.pipelines

import org.gradle.api.provider.PropertyState

class PluginDependencySpec(
  val gitPluginVersionState: PropertyState<String>,
  val workflowApiPluginVersionState: PropertyState<String>,
  val workflowBasicStepsPluginVersionState: PropertyState<String>,
  val workflowCpsPluginVersionState: PropertyState<String>,
  val workflowDurableTaskStepPluginVersionState: PropertyState<String>,
  val workflowGlobalCpsLibraryPluginVersionState: PropertyState<String>,
  val workflowJobPluginVersionState: PropertyState<String>,
  val workflowMultibranchPluginVersionState: PropertyState<String>,
  val workflowScmStepPluginVersionState: PropertyState<String>,
  val workflowStepApiPluginVersionState: PropertyState<String>,
  val workflowSupportPluginVersionState: PropertyState<String>
) {

  private val additionalDependencies: MutableList<PluginDependency> = mutableListOf()

  /**
   * Helper for adding plugins from the `com.cloudbees.jenkins.plugins` group.
   */
  fun cloudbees(name: String, version: String) = dependency("com.cloudbees.jenkins.plugins", name, version)

  /**
   * Helper for adding plugins from the `org.jenkins-ci.plugins.workflow` group.
   */
  fun workflow(name: String, version: String) = dependency("org.jenkins-ci.plugins.workflow", name, version)

  /**
   * Helper for adding plugins from the `org.jvnet.hudson.plugins` group.
   */
  fun jvnet(name: String, version: String) = dependency("org.jvnet.hudson.plugins", name, version)

  /**
   * Helper for adding plugins from the `org.jenkins-ci.plugins` group.
   */
  fun jenkinsCi(name: String, version: String) = dependency("org.jenkins-ci.plugins", name, version)

  /**
   * Helper for adding plugins from the `io.jenkins.blueocean` group.
   */
  fun blueocean(name: String, version: String) = dependency("io.jenkins.blueocean", name, version)

  /**
   * Adds a jenkinsCi dependency with the specified
   */
  fun dependency(group: String, name: String, version: String) {
    additionalDependencies.add(PluginDependency(group, name, version))
  }

  var gitPluginVersion: String
    get() = gitPluginVersionState.get()
    set(value) = gitPluginVersionState.set(value)

  var workflowApiPluginVersion: String
    get() = workflowApiPluginVersionState.get()
    set(value) = workflowApiPluginVersionState.set(value)

  var workflowBasicStepsPluginVersion: String
    get() = workflowBasicStepsPluginVersionState.get()
    set(value) = workflowBasicStepsPluginVersionState.set(value)

  var workflowCpsPluginVersion: String
    get() = workflowCpsPluginVersionState.get()
    set(value) = workflowCpsPluginVersionState.set(value)

  /**
   * Shared pipeline libraries version.
   * @see <a href="https://plugins.jenkins.io/workflow-cps-global-lib"></a>
   */
  var workflowCpsGlobalLibraryPluginVersion: String
    get() = workflowGlobalCpsLibraryPluginVersionState.get()
    set(value) = workflowGlobalCpsLibraryPluginVersionState.set(value)

  var workflowDurableTaskStepPluginVersion: String
    get() = workflowDurableTaskStepPluginVersionState.get()
    set(value) = workflowDurableTaskStepPluginVersionState.set(value)

  var workflowJobPluginVersion: String
    get() = workflowJobPluginVersionState.get()
    set(value) = workflowJobPluginVersionState.set(value)

  var workflowMultibranchPluginVersion: String
    get() = workflowMultibranchPluginVersionState.get()
    set(value) = workflowMultibranchPluginVersionState.set(value)

  var workflowScmStepPluginVersion: String
    get() = workflowScmStepPluginVersionState.get()
    set(value) = workflowScmStepPluginVersionState.set(value)

  var workflowStepApiPluginVersion: String
    get() = workflowStepApiPluginVersionState.get()
    set(value) = workflowStepApiPluginVersionState.set(value)

  var workflowSupportPluginVersion: String
    get() = workflowSupportPluginVersionState.get()
    set(value) = workflowSupportPluginVersionState.set(value)

  private fun gitPluginDependency() = "org.jenkins-ci.plugins:git:$gitPluginVersion"
  private fun workflowApiPluginDependency() = "org.jenkins-ci.plugins.workflow:workflow-api:$workflowApiPluginVersion"
  private fun workflowBasicStepsPluginDependency() = "org.jenkins-ci.plugins.workflow:workflow-basic-steps:$workflowBasicStepsPluginVersion"
  private fun workflowCpsPluginDependency() = "org.jenkins-ci.plugins.workflow:workflow-cps:$workflowCpsPluginVersion"
  private fun workflowDurableTaskStepPluginDependency() = "org.jenkins-ci.plugins.workflow:workflow-cps-global-lib:$workflowCpsGlobalLibraryPluginVersion"
  private fun workflowGlobalCpsLibraryPluginDependency() = "org.jenkins-ci.plugins.workflow:workflow-durable-task-step:$workflowDurableTaskStepPluginVersion"
  private fun workflowJobPluginDependency() = "org.jenkins-ci.plugins.workflow:workflow-job:$workflowJobPluginVersion"
  private fun workflowMultibranchPluginDependency() = "org.jenkins-ci.plugins.workflow:workflow-multibranch:$workflowMultibranchPluginVersion"
  private fun workflowScmStepPluginDependency() = "org.jenkins-ci.plugins.workflow:workflow-scm-step:$workflowScmStepPluginVersion"
  private fun workflowStepApiPluginDependency() = "org.jenkins-ci.plugins.workflow:workflow-step-api:$workflowStepApiPluginVersion"
  private fun workflowSupportPluginDependency() = "org.jenkins-ci.plugins.workflow:workflow-support:$workflowSupportPluginVersion"

  fun pluginDependencies(): List<PluginDependency> {
    val workflowPluginDependencies: List<PluginDependency> = listOf(
        gitPluginDependency(),
        workflowApiPluginDependency(),
        workflowBasicStepsPluginDependency(),
        workflowCpsPluginDependency(),
        workflowDurableTaskStepPluginDependency(),
        workflowGlobalCpsLibraryPluginDependency(),
        workflowJobPluginDependency(),
        workflowMultibranchPluginDependency(),
        workflowScmStepPluginDependency(),
        workflowStepApiPluginDependency(),
        workflowSupportPluginDependency()
    ).map { PluginDependency.fromString(it) }

    return additionalDependencies + workflowPluginDependencies
  }
}
