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

  /**
   * Jenkins BOM version.
   * Defaults to [SharedLibraryDefaults.BOM_VERSION], which matches the [SharedLibraryDefaults.CORE_VERSION] LTS line.
   * The plugin automatically adds `io.jenkins.tools.bom:bom-{MAJOR}.{MINOR}.x:{bomVersion}` as a platform
   * to the `jenkinsPlugin` configuration — no manual `jenkinsPlugin(platform(...))` call is needed.
   *
   * Override when upgrading to a different Jenkins LTS line:
   * ```kotlin
   * sharedLibrary {
   *     jenkins {
   *         version = "2.492.3"
   *         testHarnessVersion = "2397.v5e2b_42e5e01c"
   *         bomVersion = "3463.v23b_7bb_b_b_66d5"
   *     }
   * }
   * ```
   */
  abstract val bomVersion: Property<String>
}
