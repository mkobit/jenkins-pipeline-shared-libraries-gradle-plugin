package com.mkobit.jenkins.pipelines

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
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
 *     plugins {
 *         plugin("org.jenkins-ci.plugins.workflow:workflow-multibranch")
 *     }
 * }
 * ```
 *
 * The built-in `integrationTest` suite is wired automatically. Additional suites opt in by
 * setting [JenkinsTestSuiteExtension.enabled] to `true` on the suite's extension:
 * ```kotlin
 * testing {
 *     suites {
 *         register<JvmTestSuite>("integrationTestKotest") {
 *             jenkins.enabled = true
 *         }
 *     }
 * }
 * ```
 */
@Suppress("UnstableApiUsage")
abstract class SharedLibraryExtension
  @Inject
  constructor(
    private val objects: ObjectFactory,
  ) {
    val jenkins: JenkinsVersions = objects.newInstance(JenkinsVersions::class)

    /** Configures the Jenkins core and test-harness versions. */
    fun jenkins(action: Action<in JenkinsVersions>) = action.execute(jenkins)

    /** Jenkins HPI/JPI plugin dependencies declared via [plugins]. */
    val plugins: JenkinsPlugins = objects.newInstance<JenkinsPlugins>()

    /**
     * Declares Jenkins HPI/JPI plugin dependencies.
     *
     * ```kotlin
     * sharedLibrary {
     *     plugins {
     *         plugin("org.jenkins-ci.plugins.workflow:workflow-multibranch")
     *         plugin("org.6wind.jenkins:lockable-resources:2.18")
     *     }
     * }
     * ```
     */
    fun plugins(action: Action<in JenkinsPlugins>) = action.execute(plugins)

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

    /**
     * Whether the shared library is registered as implicit in embedded Jenkins.
     * Defaults to `true` — pipeline scripts can call vars directly without a `@Library` annotation.
     *
     * Set to `false` to require an explicit `@Library` declaration in every pipeline:
     * ```kotlin
     * sharedLibrary {
     *     libraryName = "my-pipeline-lib"
     *     implicit = false  // pipelines must use @Library('my-pipeline-lib')
     * }
     * ```
     */
    abstract val implicit: Property<Boolean>

    /**
     * Maximum number of Jenkins test suites that may execute concurrently within this project.
     * Controls the `JenkinsTestSuiteService` build-service slot shared by all suites wired
     * via [withJenkins]. Defaults to `1` (safe on any machine); increase on hosts with more
     * RAM — allow roughly 4 GiB per additional parallel slot.
     *
     * ```kotlin
     * sharedLibrary {
     *     maxParallelJenkinsTests = 2
     * }
     * ```
     */
    abstract val maxParallelJenkinsTests: Property<Int>

    /**
     * Opts [suite] into full Jenkins test-harness wiring.
     *
     * Prefer setting `jenkins.enabled = true` directly on the suite inside its
     * `register<JvmTestSuite>` block — it is idempotent and does not require a reference to
     * `sharedLibrary`:
     * ```kotlin
     * register<JvmTestSuite>("integrationTestKotest") {
     *     jenkins.enabled = true
     * }
     * ```
     */
    @Deprecated(
      message = "Set jenkins.enabled = true on the suite directly. Will be removed in 0.13.0.",
      level = DeprecationLevel.WARNING,
      replaceWith = ReplaceWith(expression = "jenkins.enabled = true"), // todo: double check this actually works
    )
    fun withJenkins(suite: JvmTestSuite) {
      // todo: use gradle warnings api, add test to remove in 0.13.0 release
      suite.jenkins.enabled.set(true)
    }
  }
