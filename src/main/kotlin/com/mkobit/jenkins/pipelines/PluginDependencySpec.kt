package com.mkobit.jenkins.pipelines

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.listProperty
import javax.inject.Inject

/**
 * Specifies the Jenkins plugin dependencies to include.
 * @property workflowApiPluginVersion [Pipeline: API](https://plugins.jenkins.io/workflow-api) plugin version
 * @property workflowBasicStepsPluginVersion [Pipeline: Basic Steps](https://plugins.jenkins.io/workflow-basic-steps) plugin version
 * @property workflowCpsPluginVersion [Pipeline: Groovy](https://plugins.jenkins.io/workflow-cps) plugin version
 * @property workflowDurableTaskStepPluginVersion [Durable Task](https://plugins.jenkins.io/durable-task) plugin version
 * @property workflowCpsGlobalLibraryPluginVersion [Pipeline: Shared Groovy Libraries](https://plugins.jenkins.io/workflow-cps-global-lib)
 * @property workflowJobPluginVersion [Pipeline: Job](https://plugins.jenkins.io/workflow-job) plugin version
 * @property workflowMultibranchPluginVersion [Pipeline: Multibranch](https://plugins.jenkins.io/workflow-multibranch) plugin version
 * @property workflowScmStepPluginVersion [Pipeline: SCM Step](https://plugins.jenkins.io/workflow-scm-step) plugin version
 * @property workflowStepApiPluginVersion [Pipeline: Step API](https://plugins.jenkins.io/workflow-step-api) plugin version
 * @property workflowSupportPluginVersion [Pipeline: Supporting APIs](https://plugins.jenkins.io/workflow-support) plugin version
 */
open class PluginDependencySpec
  @Inject
  constructor(
    val workflowApiPluginVersion: Property<String>,
    val workflowBasicStepsPluginVersion: Property<String>,
    val workflowCpsPluginVersion: Property<String>,
    val workflowDurableTaskStepPluginVersion: Property<String>,
    val workflowCpsGlobalLibraryPluginVersion: Property<String>,
    val workflowJobPluginVersion: Property<String>,
    val workflowMultibranchPluginVersion: Property<String>,
    val workflowScmStepPluginVersion: Property<String>,
    val workflowStepApiPluginVersion: Property<String>,
    val workflowSupportPluginVersion: Property<String>,
    private val objectFactory: ObjectFactory
  ) {
  // .empty() needed due to https://github.com/gradle/gradle/issues/7485
  private val additionalDependencies: ListProperty<PluginDependency> = objectFactory.listProperty<PluginDependency>().empty()

  /**
   * Adds a dependency with the provided coordinates.
   * @param group the group of the plugin artifact
   * @param name the name of the plugin artifact
   * @param version the version of the plugin to use
   */
  fun dependency(
      group: String,
      name: String,
      version: String
  ) {
    additionalDependencies.add(PluginDependency(group, name, version))
  }

  private fun workflowApiPluginDependency(): Provider<String> =
    workflowApiPluginVersion.map { "org.jenkins-ci.plugins.workflow:workflow-api:$it" }

  private fun workflowBasicStepsPluginDependency(): Provider<String> =
    workflowBasicStepsPluginVersion.map { "org.jenkins-ci.plugins.workflow:workflow-basic-steps:$it" }

  private fun workflowCpsPluginDependency(): Provider<String> =
    workflowCpsPluginVersion.map { "org.jenkins-ci.plugins.workflow:workflow-cps:$it" }

  private fun workflowDurableTaskStepPluginDependency(): Provider<String> =
    workflowDurableTaskStepPluginVersion.map { "org.jenkins-ci.plugins.workflow:workflow-durable-task-step:$it" }

  private fun workflowGlobalCpsLibraryPluginDependency(): Provider<String> =
    workflowCpsGlobalLibraryPluginVersion.map { "org.jenkins-ci.plugins.workflow:workflow-cps-global-lib:$it" }

  private fun workflowJobPluginDependency(): Provider<String> =
    workflowJobPluginVersion.map { "org.jenkins-ci.plugins.workflow:workflow-job:$it" }

  private fun workflowMultibranchPluginDependency(): Provider<String> =
    workflowMultibranchPluginVersion.map { "org.jenkins-ci.plugins.workflow:workflow-multibranch:$it" }

  private fun workflowScmStepPluginDependency(): Provider<String> =
    workflowScmStepPluginVersion.map { "org.jenkins-ci.plugins.workflow:workflow-scm-step:$it" }

  private fun workflowStepApiPluginDependency(): Provider<String> =
    workflowStepApiPluginVersion.map { "org.jenkins-ci.plugins.workflow:workflow-step-api:$it" }

  private fun workflowSupportPluginDependency(): Provider<String> =
    workflowSupportPluginVersion.map { "org.jenkins-ci.plugins.workflow:workflow-support:$it" }

  fun pluginDependencies(): ListProperty<PluginDependency> {
    // .empty() needed due to https://github.com/gradle/gradle/issues/7485
    val dependencies: ListProperty<PluginDependency> = objectFactory.listProperty<PluginDependency>().empty()
    dependencies.addAll(additionalDependencies)
    listOf(
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
    ).map { provider ->
      provider.map { PluginDependency.fromString(it) }
    }.forEach { dependencies.add(it) }

    return dependencies
  }
}
