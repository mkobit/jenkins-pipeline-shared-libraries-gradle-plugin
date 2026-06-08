package com.mkobit.jenkins.pipelines

import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.getByType

/**
 * Extension added to every [JvmTestSuite] by the `shared-library` plugin.
 *
 * When [useTestHarness] is `true` the suite receives full Jenkins test-harness wiring:
 * `jenkins-test-harness` on the implementation classpath, HPI archives and the WAR on the
 * test runtime classpath, per-suite `WarExploder` output directory, system-property injectors
 * for the library name/location/WAR path, and JVM flags required by Jenkins' reflection-heavy
 * internals.
 *
 * Set [useTestHarness] directly inside a `register<JvmTestSuite>` block:
 * ```kotlin
 * testing {
 *     suites {
 *         register<JvmTestSuite>("integrationTestKotest") {
 *             jenkins.useTestHarness = true
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
  abstract val useTestHarness: Property<Boolean>
}

/**
 * Returns this suite's [JenkinsTestSuiteExtension].
 *
 * This accessor exists because Gradle does not generate KotlinDSL accessors for extensions
 * registered on [JvmTestSuite] instances via
 * [configureEach][org.gradle.api.DomainObjectCollection.configureEach] — only project-level
 * extensions and named container elements receive generated accessors.
 * See [gradle/gradle#28162](https://github.com/gradle/gradle/issues/28162).
 *
 * The extension is registered eagerly by the `shared-library` plugin for every [JvmTestSuite],
 * so this accessor is always available after the plugin is applied:
 * ```kotlin
 * register<JvmTestSuite>("integrationTestKotest") {
 *     jenkins.useTestHarness = true
 * }
 * ```
 */
@Suppress("UnstableApiUsage")
val JvmTestSuite.jenkins: JenkinsTestSuiteExtension
  get() = (this as ExtensionAware).extensions.getByType<JenkinsTestSuiteExtension>()
