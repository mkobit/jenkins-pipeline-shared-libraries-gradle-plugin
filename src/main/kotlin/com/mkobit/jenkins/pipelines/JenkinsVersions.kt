package com.mkobit.jenkins.pipelines

import org.gradle.api.provider.Property

abstract class JenkinsVersions {
  /** Jenkins core version — governs `jenkins-core` compile-only dep and the `jenkins-war` used by JenkinsRule. */
  abstract val version: Property<String>

  /** `jenkins-test-harness` version used in the `integrationTest` suite. */
  abstract val testHarnessVersion: Property<String>
}
