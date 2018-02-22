package com.mkobit.jenkins.pipelines

import com.mkobit.jenkins.pipelines.http.Authentication
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import java.net.URL

/**
 * Targets a specific Jenkins instance for integration.
 * @param baseUrl the base URI of the Jenkins instance to target for integration.
 * @param authentication the authentication provider
 */
open class JenkinsIntegrationExtension(
  val baseUrl: Property<URL>,
  val authentication: Property<Authentication>,
  val downloadDirectory: DirectoryProperty
)
