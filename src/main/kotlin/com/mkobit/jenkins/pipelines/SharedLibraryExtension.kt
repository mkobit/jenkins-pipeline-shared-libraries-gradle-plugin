package com.mkobit.jenkins.pipelines

import org.gradle.api.Action
import org.gradle.api.provider.Property

/**
 * Extension for the [SharedLibraryPlugin].
 */
open class SharedLibraryExtension(
  private val groovyVersionState: Property<String>,
  private val coreVersionState: Property<String>,
  private val pipelineTestUnitVersionState: Property<String>,
  private val testHarnessVersionState: Property<String>,
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

  var pipelineTestUnitVersion: String
    get() = pipelineTestUnitVersionState.get()
    set(value) = pipelineTestUnitVersionState.set(value)

  var testHarnessVersion: String
    get() = testHarnessVersionState.get()
    set(value) = testHarnessVersionState.set(value)

  fun coreDependency() = "org.jenkins-ci.main:jenkins-core:$coreVersion"
  fun groovyDependency() = "org.codehaus.groovy:groovy:$groovyVersion"
  fun pipelineUnitDependency(): String = "com.lesfurets:jenkins-pipeline-unit:$pipelineTestUnitVersion"
  fun testHarnessDependency() = "org.jenkins-ci.main:jenkins-test-harness:$testHarnessVersion"
  // See https://issues.jenkins-ci.org/browse/JENKINS-24064 and 2.64 release notes about war-for-test not being needed in some cases
  // Also, see https://github.com/jenkinsci/jenkins/pull/2899/files
  // https://github.com/jenkinsci/plugin-pom/pull/40/files shows how the new plugin-pom does the jenkins.war generation
  fun jenkinsWar() = "org.jenkins-ci.main:jenkins-war:$coreVersion"

  @Suppress("UNUSED")
  fun pluginDependencies(action: Action<in PluginDependencySpec>) {
    action.execute(pluginDependencySpec)
  }

  internal fun pluginDependencies(): PluginDependencySpec = pluginDependencySpec
}
