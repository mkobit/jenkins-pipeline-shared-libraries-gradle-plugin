package com.mkobit.jenkins.pipelines

import org.gradle.api.provider.Property

/**
 * Jenkins core version coordinates for the `sharedLibrary` plugin.
 *
 * Defaults are sourced from [SharedLibraryDefaults]. Override inside the `jenkins { }` block:
 *
 * ```kotlin
 * sharedLibrary {
 *     jenkins {
 *         version = "2.492.1"
 *         bomVersion = "3463.v23b_7bb_b_b_66d5"
 *     }
 * }
 * ```
 *
 * The `jenkins-test-harness` version is pinned automatically by the Jenkins BOM — no explicit
 * override is needed. To use a newer harness than the BOM pins, declare it in your suite's
 * `dependencies` block: `implementation("org.jenkins-ci.main:jenkins-test-harness:VERSION")`.
 */
abstract class JenkinsVersions {
  /** Jenkins core version — governs the `jenkins-core` compile-only dependency and the `jenkins-war` artifact used by `JenkinsRule`. */
  abstract val version: Property<String>

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
   *         bomVersion = "3463.v23b_7bb_b_b_66d5"
   *     }
   * }
   * ```
   */
  abstract val bomVersion: Property<String>
}
