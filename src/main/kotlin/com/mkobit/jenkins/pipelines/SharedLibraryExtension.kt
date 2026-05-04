package com.mkobit.jenkins.pipelines

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Extension for the `com.mkobit.jenkins.pipelines.shared-library` plugin.
 *
 * All properties have sensible defaults (see [SharedLibraryDefaults]) and are optional.
 *
 * ```kotlin
 * sharedLibrary {
 *     jenkins {
 *         version = "2.492.3"
 *         testHarnessVersion = "2397.v5992a_d47f025"
 *     }
 *     pipelineUnitVersion = "1.29"
 * }
 * ```
 *
 * The built-in `integrationTest` suite is wired automatically. Additional suites
 * (JUnit Jupiter, Spock, Kotest, etc.) must opt in by calling [jenkinsTestRunnerSuite]
 * inside their `register<JvmTestSuite>` block:
 * ```kotlin
 * testing {
 *     suites {
 *         register<JvmTestSuite>("integrationTestKotest") {
 *             sharedLibrary.jenkinsTestRunnerSuite(this)
 *         }
 *     }
 * }
 * ```
 */
abstract class SharedLibraryExtension
  @Inject
  constructor(
    objects: ObjectFactory,
  ) {
    val jenkins: JenkinsVersions = objects.newInstance(JenkinsVersions::class.java)

    /** Configures the Jenkins core and test-harness versions. */
    fun jenkins(action: Action<in JenkinsVersions>) = action.execute(jenkins)

    /** `com.lesfurets:jenkins-pipeline-unit` version used in the `test` suite. */
    abstract val pipelineUnitVersion: Property<String>

    /**
     * When `true` (default), generates `SharedLibraryAutoRegistrar.java` and registers the SezPoz
     * annotation processor so Jenkins auto-registers the shared library at embedded Jenkins startup.
     * No explicit `GlobalLibraries.get().setLibraries(...)` call is needed in test code.
     *
     * Set to `false` to revert to the manual registration pattern:
     * ```kotlin
     * sharedLibrary {
     *     autoRegisterLibrary = false
     * }
     * ```
     */
    abstract val autoRegisterLibrary: Property<Boolean>

    private var testSuiteWirer: ((JvmTestSuite) -> Unit)? = null

    internal fun setTestSuiteWirer(action: (JvmTestSuite) -> Unit) {
      testSuiteWirer = action
    }

    /**
     * Applies full Jenkins test-harness wiring to [suite] — identical to the built-in
     * `integrationTest` suite: `jenkins-test-harness`, HPI classpath, WAR path,
     * system properties, JVM opens, and `mustRunAfter("test")` ordering.
     */
    fun jenkinsTestRunnerSuite(suite: JvmTestSuite) {
      checkNotNull(testSuiteWirer) {
        "jenkinsTestRunnerSuite() called before plugin wiring is complete"
      }.invoke(suite)
    }
  }
