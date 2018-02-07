package com.mkobit.jenkins.pipelines

import com.mkobit.jenkins.pipelines.auth.Credentials
import org.gradle.api.provider.Property
import java.net.URI

/**
 * Targets a specific Jenkins instance for integration.
 * @param instanceUri the URI of the Jenkins instance to target for integration.
 * @param credentials the provider of credentials to for authentication
 */
open class JenkinsIntegrationExtension(
  var instanceUri: URI?,
  var credentials: Property<Credentials>
)
