package com.mkobit.jenkins.pipelines

import com.mkobit.jenkins.pipelines.auth.Credentials
import org.gradle.api.provider.Property
import java.net.URI

/**
 * @param instanceUri the URI of the Jenkins instance to target for integration.
 */
open class JenkinsIntegrationExtension(
  var instanceUri: URI?,
  var credentials: Property<out Credentials>
)
