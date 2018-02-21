package com.mkobit.jenkins.pipelines

import com.mkobit.jenkins.pipelines.http.Authentication
import org.gradle.api.provider.Property
import java.net.URL

/**
 * Targets a specific Jenkins instance for integration.
 * @param baseUrl the base URI of the Jenkins instance to target for integration.
 * @param authentication the authentication provider
 */
open class JenkinsIntegrationExtension(
  var baseUrl: URL?,
  var authentication: Property<Authentication>
)
