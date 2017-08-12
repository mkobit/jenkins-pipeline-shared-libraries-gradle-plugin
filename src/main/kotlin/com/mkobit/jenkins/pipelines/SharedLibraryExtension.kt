package com.mkobit.jenkins.pipelines

import org.gradle.api.Action
import org.gradle.api.provider.PropertyState

open class SharedLibraryExtension(
  val groovyVersionState: PropertyState<String>,
  val coreVersionState: PropertyState<String>,
  val globalLibPluginVersionState: PropertyState<String>
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

  fun pluginDependencies(action: Action<in PluginDependencySpec>) {
    action.execute(pluginDependencySpec)
  }

  fun pluginDependencies(): List<PluginDependency> = pluginDependencySpec.getDependencies()
}
