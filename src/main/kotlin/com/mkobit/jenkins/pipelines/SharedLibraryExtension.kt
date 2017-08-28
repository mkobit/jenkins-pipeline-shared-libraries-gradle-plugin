package com.mkobit.jenkins.pipelines

import org.gradle.api.Action
import org.gradle.api.provider.PropertyState

open class SharedLibraryExtension(
  val groovyVersionState: PropertyState<String>,
  val coreVersionState: PropertyState<String>,
  val pipelineTestUnitVersionState: PropertyState<String>,
  val testHarnessVersionState: PropertyState<String>,
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

  private val pluginDependencySpec: PluginDependencySpec = DefaultPluginDependencySpec()

  /**
   * Groovy version.
   */
  var groovyVersion: String
    get() = groovyVersionState.get()
    set(value) = groovyVersionState.set(value)

  /**
   * Jenkins version.
   */
  var coreVersion: String
    get() = coreVersionState.get()
    set(value) = coreVersionState.set(value)

  var pipelineTestUnitVersion: String?
    get() = pipelineTestUnitVersionState.orNull
    set(value) = pipelineTestUnitVersionState.set(value)

  var testHarnessVersion: String
    get() = testHarnessVersionState.get()
    set(value) = testHarnessVersionState.set(value)

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

  fun coreDependency() = "org.jenkins-ci.main:jenkins-core:$coreVersion"
  fun groovyDependency() = "org.codehaus.groovy:groovy:$groovyVersion"
  fun pipelineUnitDependency(): String? = pipelineTestUnitVersion?.let { "com.lesfurets:jenkins-pipeline-unit:$it" }
  fun testHarnessDependency() = "org.jenkins-ci.main:jenkins-test-harness:$testHarnessVersion"
  private fun gitPluginDependency() = "org.jenkins-ci.plugins:git:$gitPluginVersion"
  private fun workflowApiPluginDependency() = "org.jenkins-ci.plugins.workflow:workflow-api:$workflowApiPluginVersion"
  private fun workflowBasicStepsPluginDependency() = "org.jenkins-ci.plugins.workflow:workflow-basic-steps:$workflowBasicStepsPluginVersion"
  private fun workflowCpsPluginDependency() = "org.jenkins-ci.plugins.workflow:workflow-cps:$workflowCpsPluginVersion"
  private fun workflowDurableTaskStepPluginDependency() = "org.jenkins-ci.plugins.workflow:workflow-cps-global-lib:$workflowDurableTaskStepPluginVersion"
  private fun workflowGlobalCpsLibraryPluginPluginDependency() = "org.jenkins-ci.plugins.workflow:workflow-durable-task-step:$workflowCpsGlobalLibraryPluginVersion"
  private fun workflowJobPluginDependency() = "org.jenkins-ci.plugins.workflow:workflow-job:$workflowJobPluginVersion"
  private fun workflowMultibranchPluginDependency() = "org.jenkins-ci.plugins.workflow:workflow-multibranch:$workflowMultibranchPluginVersion"
  private fun workflowScmStepPluginDependency() = "org.jenkins-ci.plugins.workflow:workflow-scm-step:$workflowScmStepPluginVersion"
  private fun workflowStepApiPluginDependency() = "org.jenkins-ci.plugins.workflow:workflow-step-api:$workflowApiPluginVersion"
  private fun workflowSupportPluginDependency() = "org.jenkins-ci.plugins.workflow:workflow-support:$workflowSupportPluginVersion"
  // See https://issues.jenkins-ci.org/browse/JENKINS-24064 and 2.64 release notes about war-for-test not being needed in some cases
  // Also, see https://github.com/jenkinsci/jenkins/pull/2899/files
  // https://github.com/jenkinsci/plugin-pom/pull/40/files shows how the new plugin-pom does the jenkins.war generation
  fun jenkinsWar() = "org.jenkins-ci.main:jenkins-war:$coreVersion"

  fun pluginDependencies(action: Action<in PluginDependencySpec>) {
    action.execute(pluginDependencySpec)
  }

  fun pluginDependencies(): List<PluginDependency> {
    val dependenciesFromSpec = pluginDependencySpec.getDependencies()
    val workflowPluginDependencies: List<PluginDependency> = listOf(
        gitPluginDependency(),
        workflowApiPluginDependency(),
        workflowBasicStepsPluginDependency(),
        workflowCpsPluginDependency(),
        workflowDurableTaskStepPluginDependency(),
        workflowGlobalCpsLibraryPluginPluginDependency(),
        workflowJobPluginDependency(),
        workflowMultibranchPluginDependency(),
        workflowScmStepPluginDependency(),
        workflowStepApiPluginDependency(),
        workflowSupportPluginDependency()
    ).map { PluginDependency.fromString(it) }


    return dependenciesFromSpec + workflowPluginDependencies
  }
}
