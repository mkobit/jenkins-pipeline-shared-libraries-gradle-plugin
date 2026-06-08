package com.mkobit.jenkins.pipelines

import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.getByType

/**
 * Extension added to every [JvmTestSuite] by the `shared-library` plugin.
 *
 * When [enabled] is `true` the suite receives full Jenkins test-harness wiring:
 * `jenkins-test-harness` on the implementation classpath, HPI archives and the WAR on the
 * test runtime classpath, per-suite `WarExploder` output directory, system-property injectors
 * for the library name/location/WAR path, and JVM flags required by Jenkins' reflection-heavy
 * internals.
 *
 * Set [enabled] directly inside a `register<JvmTestSuite>` block:
 * ```kotlin
 * testing {
 *     suites {
 *         register<JvmTestSuite>("integrationTestKotest") {
 *             jenkins.enabled = true
 *         }
 *     }
 * }
 * ```
 *
 * The built-in `integrationTest` suite is enabled automatically.
 * The `test` suite is always disabled (it uses `jenkins-pipeline-unit`, not the full harness).
 */
@Suppress("UnstableApiUsage")
abstract class JenkinsTestSuiteExtension {
  abstract val enabled: Property<Boolean>
}

/**
 * Returns this suite's [JenkinsTestSuiteExtension].
 *
 * The extension is registered eagerly by the `shared-library` plugin for every [JvmTestSuite],
 * so this accessor is always available after the plugin is applied:
 * ```kotlin
 * register<JvmTestSuite>("integrationTestKotest") {
 *     jenkins.enabled = true
 * }
 * ```
 */
@Suppress("UnstableApiUsage")
val JvmTestSuite.jenkins: JenkinsTestSuiteExtension
  get() = (this as ExtensionAware).extensions.getByType<JenkinsTestSuiteExtension>()
