package com.mkobit.jenkins.pipelines

import org.gradle.api.Action
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

/**
 * Extension for the [SharedLibraryPlugin].
 * @property coreVersion Jenkins version
 * @property pipelineTestUnitVersion Version of the [JenkinsPipelineUnit](https://github.com/jenkinsci/JenkinsPipelineUnit)
 * library to use in unit tests
 * @property testHarnessVersion Version of the [jenkins-test-harness](https://github.com/jenkinsci/jenkins-test-harness)
 * library to use in integration tests
 */
open class SharedLibraryExtension(
  val coreVersion: Property<String>,
  val pipelineTestUnitVersion: Property<String>,
  val testHarnessVersion: Property<String>,
  private val pluginDependencySpec: PluginDependencySpec
) {

  fun coreDependency(): Provider<String> = coreVersion.map { "org.jenkins-ci.main:jenkins-core:$it" }
  fun pipelineUnitDependency(): Provider<String> = pipelineTestUnitVersion.map { "com.lesfurets:jenkins-pipeline-unit:$it" }
  fun testHarnessDependency(): Provider<String> = testHarnessVersion.map { "org.jenkins-ci.main:jenkins-test-harness:$it" }
  // See https://issues.jenkins-ci.org/browse/JENKINS-24064 and 2.64 release notes about war-for-test not being needed in some cases
  // Also, see https://github.com/jenkinsci/jenkins/pull/2899/files
  // https://github.com/jenkinsci/plugin-pom/pull/40/files shows how the new plugin-pom does the jenkins.war generation
  fun jenkinsWar(): Provider<String> = coreVersion.map { "org.jenkins-ci.main:jenkins-war:$it" }

  @Suppress("UNUSED")
  fun pluginDependencies(action: Action<in PluginDependencySpec>) {
    action.execute(pluginDependencySpec)
  }

  internal fun pluginDependencies(): PluginDependencySpec = pluginDependencySpec
}
