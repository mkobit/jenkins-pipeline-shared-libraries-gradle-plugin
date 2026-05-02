package com.mkobit.jenkins.pipelines

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
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
 * Every [JvmTestSuite] in the project automatically receives full Jenkins harness wiring —
 * `jenkins-test-harness`, HPI classpath, WAR path, system properties, and JVM opens.
 * No additional configuration is required for consumer-registered suites.
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
  }
