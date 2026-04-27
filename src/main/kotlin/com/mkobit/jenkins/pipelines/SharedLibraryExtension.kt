package com.mkobit.jenkins.pipelines

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

abstract class SharedLibraryExtension
  @Inject
  constructor(
    objects: ObjectFactory,
  ) {
    val jenkins: JenkinsVersions = objects.newInstance(JenkinsVersions::class.java)

    fun jenkins(action: Action<in JenkinsVersions>) = action.execute(jenkins)
  }
