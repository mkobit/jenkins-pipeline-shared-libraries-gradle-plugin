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
 * Additional integration test suites (JUnit Jupiter, Spock, Kotest, etc.) can be opted into
 * full Jenkins harness wiring by calling [jenkinsTestRunnerSuite] inside the suite registration:
 * ```kotlin
 * testing {
 *     suites {
 *         register<JvmTestSuite>("integrationTestJunit5") {
 *             sharedLibrary.jenkinsTestRunnerSuite(this)
 *             // ...suite-specific config...
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

    private var testSuiteWirer: ((JvmTestSuite) -> Unit)? = null

    internal fun setTestSuiteWirer(action: (JvmTestSuite) -> Unit) {
      testSuiteWirer = action
    }

    /**
     * Applies full Jenkins test-harness wiring to [suite] — identical to the built-in
     * `integrationTest` suite. Call this inside your `register<JvmTestSuite>` block to opt
     * the suite into Jenkins dependency injection, HPI classpath, WAR path, system properties,
     * and JVM opens.
     */
    fun jenkinsTestRunnerSuite(suite: JvmTestSuite) {
      checkNotNull(testSuiteWirer) {
        "jenkinsTestRunnerSuite() called before plugin wiring is complete"
      }.invoke(suite)
    }
  }
