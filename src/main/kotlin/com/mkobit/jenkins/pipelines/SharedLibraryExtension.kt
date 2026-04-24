package com.mkobit.jenkins.pipelines

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

open class SharedLibraryExtension(
  val pipelineTestUnitVersion: Property<String>,
  val testHarnessVersion: Property<String>,
) {
  internal fun pipelineUnitDependency(): Provider<String> = pipelineTestUnitVersion.map { "com.lesfurets:jenkins-pipeline-unit:$it" }

  internal fun testHarnessDependency(): Provider<String> = testHarnessVersion.map { "org.jenkins-ci.main:jenkins-test-harness:$it" }
}
