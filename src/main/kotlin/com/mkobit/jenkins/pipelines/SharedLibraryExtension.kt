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
 *         version = "2.492.1"
 *         testHarnessVersion = "2500.vb_4b_5ef084eb_4"
 *     }
 *     pipelineUnitVersion = "1.29"
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
  }
