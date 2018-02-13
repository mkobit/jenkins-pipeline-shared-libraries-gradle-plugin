package com.mkobit.jenkins.pipelines

import com.mkobit.jenkins.pipelines.http.AnonymousAuthentication
import com.mkobit.jenkins.pipelines.http.Authentication
import mu.KotlinLogging
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.property

internal open class JenkinsIntegrationPlugin : Plugin<Project> {
  companion object {
    private val LOGGER = KotlinLogging.logger {}
    private const val EXTENSION_NAME = "jenkinsIntegration"
  }
  override fun apply(target: Project) {
    target.run {
      extensions.create(
        EXTENSION_NAME,
        JenkinsIntegrationExtension::class.java,
        null,
        objects.property<Authentication>().apply { set(AnonymousAuthentication) }
      )
    }
  }
}
