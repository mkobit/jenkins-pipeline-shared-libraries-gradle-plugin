package com.mkobit.jenkins.pipelines

import org.gradle.api.Action
import org.gradle.api.provider.PropertyState

open class SharedLibraryExtension(
  val groovyVersionState: PropertyState<String>,
  val coreVersionState: PropertyState<String>,
  val globalLibPluginVersionState: PropertyState<String>,
  val testHarnessVersionState: PropertyState<String>,
  val pipelineTestUnitVersionState: PropertyState<String>
) {

  private val pluginDependencySpec: PluginDependencySpec = DefaultPluginDependencySpec()

  /**
   * Jenkins version.
   */
  var coreVersion: String
    get() = coreVersionState.get()
    set(value) = coreVersionState.set(value)

  /**
   * Shared pipeline libraries jenkinsCi version.
   * @see <a href="https://github.com/jenkinsci/workflow-cps-global-lib-jenkinsCi"></a>
   */
  var globalLibPluginVersion: String
    get() = globalLibPluginVersionState.get()
    set(value) = globalLibPluginVersionState.set(value)

  /**
   * Shared pipeline libraries jenkinsCi version.
   * @see <a href="https://github.com/jenkinsci/workflow-cps-global-lib-jenkinsCi"></a>
   */
  var groovyVersion: String
    get() = groovyVersionState.get()
    set(value) = groovyVersionState.set(value)

  var testHarnessVersion: String
    get() = testHarnessVersionState.get()
    set(value) = testHarnessVersionState.set(value)

  var pipelineTestUnitVersion: String?
    get() = pipelineTestUnitVersionState.orNull
    set(value) = pipelineTestUnitVersionState.set(value)

  fun jenkinsCoreDependency() = "org.jenkins-ci.main:jenkins-core:$coreVersion"
  fun jenkinsGlobalLibDependency() = "org.jenkins-ci.plugins.workflow:workflow-cps-global-lib:$globalLibPluginVersion"
  fun groovyDependency() = "org.codehaus.groovy:groovy:$groovyVersion"
  fun jenkinsPipelineUnitDependency(): String? = pipelineTestUnitVersion?.let { "com.lesfurets:jenkins-pipeline-unit:$it" }
  fun jenkinsTestHarnessDependency() = "org.jenkins-ci.main:jenkins-test-harness:$testHarnessVersion"
  // See https://issues.jenkins-ci.org/browse/JENKINS-24064 and 2.64 release notes about war-for-test not being needed in some cases
  // Also, see https://github.com/jenkinsci/jenkins/pull/2899/files
  // https://github.com/jenkinsci/plugin-pom/pull/40/files shows how the new plugin-pom does the jenkins.war generation
  fun jenkinsWar() = "org.jenkins-ci.main:jenkins-war:$coreVersion"

  fun pluginDependencies(action: Action<in PluginDependencySpec>) {
    action.execute(pluginDependencySpec)
  }

  fun pluginDependencies(): List<PluginDependency> = pluginDependencySpec.getDependencies()
}
