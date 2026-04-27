package com.mkobit.jenkins.pipelines

import com.mkobit.jenkins.pipelines.http.Authentication
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import java.net.URL

abstract class JenkinsIntegrationExtension {
  abstract val baseUrl: Property<URL>
  abstract val authentication: Property<Authentication>
  abstract val downloadDirectory: DirectoryProperty
}
