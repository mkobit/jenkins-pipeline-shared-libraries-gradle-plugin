package com.mkobit.jenkins.pipelines

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class SharedLibraryExtension
  @Inject
  constructor(
    objects: ObjectFactory,
  ) {
    val jenkins: JenkinsVersions = objects.newInstance(JenkinsVersions::class.java)

    fun jenkins(action: Action<in JenkinsVersions>) = action.execute(jenkins)

    /** `com.lesfurets:jenkins-pipeline-unit` version used in the `test` suite. */
    abstract val pipelineUnitVersion: Property<String>
  }
