package com.mkobit.jenkins.pipelines

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.newInstance
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
 *         bomVersion = "3463.v23b_7bb_b_b_66d5"
 *     }
 *     pipelineUnitVersion = "1.29"
 * }
 * ```
 *
 * The built-in `integrationTest` suite is wired automatically. Additional suites
 * (JUnit Jupiter, Spock, Kotest, etc.) opt in by calling [withJenkins] inside their
 * `register<JvmTestSuite>` block:
 * ```kotlin
 * testing {
 *     suites {
 *         register<JvmTestSuite>("integrationTestKotest") {
 *             sharedLibrary.withJenkins(this)
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
    val jenkins: JenkinsVersions = objects.newInstance(JenkinsVersions::class)

    /** Configures the Jenkins core and test-harness versions. */
    fun jenkins(action: Action<in JenkinsVersions>) = action.execute(jenkins)

    /** `com.lesfurets:jenkins-pipeline-unit` version used in the `test` suite. */
    abstract val pipelineUnitVersion: Property<String>

    /**
     * Name of the shared library injected into the embedded Jenkins test instance.
     * Defaults to `project.name`.
     *
     * This value is injected as the `test.library.name` system property on all Jenkins test
     * suites. Pipelines reference the library by this name:
     * ```groovy
     * @Library('my-shared-lib') _
     * ```
     * Override when the Jenkins library name must differ from the Gradle project name:
     * ```kotlin
     * sharedLibrary {
     *     libraryName = "my-shared-lib"
     * }
     * ```
     */
    abstract val libraryName: Property<String>

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

    private var jenkinsWirer: ((JvmTestSuite) -> Unit)? = null

    internal fun setJenkinsWirer(action: (JvmTestSuite) -> Unit) {
      jenkinsWirer = action
    }

    /**
     * Applies full Jenkins test-harness wiring to [suite] — identical to the built-in
     * `integrationTest` suite: `jenkins-test-harness`, HPI classpath, WAR path,
     * system properties, JVM opens, and `mustRunAfter("test")` ordering.
     */
    fun withJenkins(suite: JvmTestSuite) {
      checkNotNull(jenkinsWirer) {
        "withJenkins() called before plugin wiring is complete"
      }.invoke(suite)
    }
  }
