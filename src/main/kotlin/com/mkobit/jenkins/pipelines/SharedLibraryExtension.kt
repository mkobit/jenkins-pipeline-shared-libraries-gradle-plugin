package com.mkobit.jenkins.pipelines

import org.gradle.api.Action
import org.gradle.api.provider.PropertyState

/**
 * Extension for the [SharedLibraryPlugin].
 */
open class SharedLibraryExtension(
  val groovyVersionState: PropertyState<String>,
  val coreVersionState: PropertyState<String>,
  val pipelineTestUnitVersionState: PropertyState<String>,
  val testHarnessVersionState: PropertyState<String>,
  private val pluginDependencySpec: PluginDependencySpec
) {

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

  fun coreDependency() = "org.jenkins-ci.main:jenkins-core:$coreVersion"
  fun groovyDependency() = "org.codehaus.groovy:groovy:$groovyVersion"
  fun pipelineUnitDependency(): String? = pipelineTestUnitVersion?.let { "com.lesfurets:jenkins-pipeline-unit:$it" }
  fun testHarnessDependency() = "org.jenkins-ci.main:jenkins-test-harness:$testHarnessVersion"
  // See https://issues.jenkins-ci.org/browse/JENKINS-24064 and 2.64 release notes about war-for-test not being needed in some cases
  // Also, see https://github.com/jenkinsci/jenkins/pull/2899/files
  // https://github.com/jenkinsci/plugin-pom/pull/40/files shows how the new plugin-pom does the jenkins.war generation
  fun jenkinsWar() = "org.jenkins-ci.main:jenkins-war:$coreVersion"

  fun pluginDependencies(action: Action<in PluginDependencySpec>) {
    action.execute(pluginDependencySpec)
  }

  fun pluginDependencies(): PluginDependencySpec = pluginDependencySpec
}
