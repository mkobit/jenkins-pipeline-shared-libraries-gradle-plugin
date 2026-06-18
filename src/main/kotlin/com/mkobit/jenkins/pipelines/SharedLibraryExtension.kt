package com.mkobit.jenkins.pipelines

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.problems.ProblemGroup
import org.gradle.api.problems.ProblemId
import org.gradle.api.problems.Problems
import org.gradle.api.problems.Severity
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
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
 *     autoRegisterLibrary = true
 *     implicit = true
 *     plugins {
 *         plugin("org.jenkins-ci.plugins.workflow:workflow-multibranch")
 *     }
 * }
 * ```
 *
 * The built-in `integrationTest` suite is wired automatically. Additional suites opt in by
 * setting [JenkinsTestSuiteExtension.useTestHarness] to `true` on the suite's extension:
 * ```kotlin
 * testing {
 *     suites {
 *         register<JvmTestSuite>("integrationTestKotest") {
 *             jenkins.useTestHarness = true
 *         }
 *     }
 * }
 * ```
 */
@Suppress("UnstableApiUsage")
abstract class SharedLibraryExtension
  @Inject
  constructor(
    objects: ObjectFactory,
    project: Project,
    private val problems: Problems,
  ) {
    val jenkins: JenkinsVersions =
      objects.newInstance<JenkinsVersions>().also {
        it.version.convention(SharedLibraryDefaults.CORE_VERSION)
        it.bomVersion.convention(SharedLibraryDefaults.BOM_VERSION)
      }

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

    /** Peer shared library dependencies declared via [dependencies]. */
    val dependencies: SharedLibraryDependencies = objects.newInstance<SharedLibraryDependencies>()

    /**
     * Declares peer shared library dependencies — other shared libraries this project
     * depends on. Each peer is registered with the embedded Jenkins as a Global Library.
     *
     * ```kotlin
     * sharedLibrary {
     *     dependencies {
     *         sharedLibrary("com.example:config-lib:1.0.0")
     *         sharedLibrary(project(":config-lib"))
     *     }
     * }
     * ```
     */
    fun dependencies(action: Action<in SharedLibraryDependencies>) = action.execute(dependencies)

    /** `com.lesfurets:jenkins-pipeline-unit` version used in the `test` suite. */
    val pipelineUnitVersion: Property<String> =
      objects.property<String>().convention(SharedLibraryDefaults.PIPELINE_UNIT_VERSION)

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
    val libraryName: Property<String> =
      objects.property<String>().convention(project.name)

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
    val autoRegisterLibrary: Property<Boolean> =
      objects.property<Boolean>().convention(true)

    /**
     * Controls the
     * [implicit][org.jenkinsci.plugins.workflow.libs.LibraryConfiguration.implicit]
     * flag on the shared library registered in the embedded Jenkins test instance.
     * When `true` (default), pipeline scripts can call vars without an explicit `@Library`
     * declaration.
     *
     * ```kotlin
     * sharedLibrary {
     *     implicit = true  // default
     * }
     * ```
     */
    val implicit: Property<Boolean> =
      objects.property<Boolean>().convention(true)

    /**
     * Maximum number of Jenkins test suites that may execute concurrently within this project.
     * Controls the `JenkinsTestSuiteService` build-service slot shared by all suites wired
     * via [JenkinsTestSuiteExtension.useTestHarness]. Defaults to `1` (safe on any machine);
     * increase on hosts with more RAM — allow roughly 4 GiB per additional parallel slot.
     *
     * ```kotlin
     * sharedLibrary {
     *     maxParallelJenkinsTests = 2
     * }
     * ```
     */
    val maxParallelJenkinsTests: Property<Int> =
      objects.property<Int>().convention(1)

    /**
     * Opts [suite] into full Jenkins test-harness wiring.
     *
     * Prefer setting `jenkins.useTestHarness = true` directly on the suite inside its
     * `register<JvmTestSuite>` block — it is idempotent and does not require a reference to
     * `sharedLibrary`:
     * ```kotlin
     * register<JvmTestSuite>("integrationTestKotest") {
     *     jenkins.useTestHarness = true
     * }
     * ```
     */
    @Deprecated(
      message = "Set jenkins.useTestHarness = true on the suite directly. Will be removed in 0.13.0.",
      level = DeprecationLevel.WARNING,
      replaceWith =
        ReplaceWith(
          expression = "jenkins.useTestHarness = true",
          imports = arrayOf("com.mkobit.jenkins.pipelines.jenkins"),
        ),
    )
    fun withJenkins(suite: JvmTestSuite) {
      problems.reporter.report(WITH_JENKINS_DEPRECATED_ID) {
        severity(Severity.WARNING)
        details("sharedLibrary.withJenkins(suite) is deprecated and will be removed in 0.13.0.")
        solution("Set jenkins.useTestHarness = true directly on the suite inside its register block.")
      }
      suite.jenkins.useTestHarness.set(true)
    }

    companion object {
      private val GROUP =
        ProblemGroup.create("com.mkobit.jenkins.pipelines", "Jenkins Pipeline Shared Libraries")
      private val WITH_JENKINS_DEPRECATED_ID =
        ProblemId.create("with-jenkins-deprecated", "sharedLibrary.withJenkins() is deprecated", GROUP)
    }
  }
