package com.mkobit.jenkins.pipelines

import org.gradle.api.provider.Property

/**
 * Jenkins core and test-harness version coordinates for the `sharedLibrary` plugin.
 *
 * Defaults are sourced from [SharedLibraryDefaults]. Override inside the `jenkins { }` block:
 *
 * ```kotlin
 * sharedLibrary {
 *     jenkins {
 *         version = "2.492.1"
 *         testHarnessVersion = "2500.vb_4b_5ef084eb_4"
 *     }
 * }
 * ```
 */
abstract class JenkinsVersions {
  /** Jenkins core version — governs the `jenkins-core` compile-only dependency and the `jenkins-war` artifact used by `JenkinsRule`. */
  abstract val version: Property<String>

  /** `jenkins-test-harness` version added to the `integrationTest` compile and runtime classpaths. */
  abstract val testHarnessVersion: Property<String>
}
