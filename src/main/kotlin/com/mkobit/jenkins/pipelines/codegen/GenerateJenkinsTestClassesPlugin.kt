package com.mkobit.jenkins.pipelines.codegen

import mu.KotlinLogging
import org.gradle.api.Plugin
import org.gradle.api.Project

open class GenerateJenkinsTestClassesPlugin : Plugin<Project> {
  companion object {
    private val logger = KotlinLogging.logger {}
  }

  override fun apply(project: Project) {
  }
}
