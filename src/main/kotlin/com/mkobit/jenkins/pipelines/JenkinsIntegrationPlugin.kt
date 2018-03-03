package com.mkobit.jenkins.pipelines

import com.mkobit.jenkins.pipelines.http.AnonymousAuthentication
import com.mkobit.jenkins.pipelines.http.Authentication
import com.mkobit.jenkins.pipelines.http.internal.connect
import com.mkobit.jenkins.pipelines.http.internal.downloadGdsl
import com.mkobit.jenkins.pipelines.http.internal.retrievePluginManagerData
import mu.KotlinLogging
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.Response
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.property
import java.net.URL

internal open class JenkinsIntegrationPlugin : Plugin<Project> {
  companion object {
    private val LOGGER = KotlinLogging.logger {}
    private const val EXTENSION_NAME = "jenkinsIntegration"
  }
  override fun apply(target: Project) {
    target.run {
      val integration = extensions.create(
        EXTENSION_NAME,
        JenkinsIntegrationExtension::class.java,
        objects.property<URL>(),
        objects.property<Authentication>().apply { set(AnonymousAuthentication) },
        layout.directoryProperty(layout.buildDirectory.dir("jenkinsIntegrationDownloads"))
      )

      tasks {
        "retrieveJenkinsGdsl" {
          setupRetrieveJenkinsGdsl(integration, this)
        }

        "retrieveJenkinsPluginData" {
          setupRetrieveJenkinsPluginData(integration, this)
        }

        "retrieveJenkinsVersion" {
          setupRetrieveJenkinsVersion(integration, this)
        }
      }
    }
  }

  private fun setupRetrieveJenkinsGdsl(integration: JenkinsIntegrationExtension, task: Task) {
    task.apply {
      description = "Downloads the Jenkins Pipeline GDSL from the Jenkins instance"
      inputs.property("url", integration.baseUrl)
      outputs.file(integration.downloadDirectory.file("idea.gdsl"))
      outputs.upToDateWhen { false }
      doLast {
        downloadGdsl(HttpUrl.get(
          integration.baseUrl.get())!!,
          integration.authentication.getOrElse(AnonymousAuthentication)
        ).use { response ->
          if (!response.isSuccessful) throw GradleException("Error retrieving GDSL. ${response.statusLineAsMessage}")
          integration.downloadDirectory
            .file("idea.gdsl")
            .get()
            .asFile.
            writeBytes(response.body()!!.bytes())
        }
      }
    }
  }
  
  private fun setupRetrieveJenkinsPluginData(integration: JenkinsIntegrationExtension, task: Task) {
    task.apply {
      description = "Downloads the Jenkins plugin data from the Jenkins instance"
      inputs.property("url", integration.baseUrl)
      outputs.file(integration.downloadDirectory.file("plugins.json"))
      outputs.upToDateWhen { false }
      doLast {
        retrievePluginManagerData(HttpUrl.get(
          integration.baseUrl.get())!!,
          integration.authentication.getOrElse(AnonymousAuthentication)
        ).use { response ->
          if (!response.isSuccessful) {
            val userDetails = response.headers().xYouAreAuthenticatedAs?.let { "User: $it" }
            val requiredPermission = response.headers().xRequiredPermission?.let { "Permission required: $it" }
            val permissionImpliedBy = response.headers().xPermissionImpliedBy.run {
              if (isNotEmpty()) {
                joinToString(System.lineSeparator()) { "Permission implied by: $it" }
              } else {
                null
              }
            }
            val errorSubject = when (response.code()) {
              401 -> "Unable to authenticate due to invalid password/token ${response.statusLineAsMessage}"
              403 -> "Unauthorized to retrieve plugin data ${response.statusLineAsMessage}"
              else -> "Error downloading plugin data ${response.statusLineAsMessage}"
            }
            val errorMessage = listOf(errorSubject, userDetails, requiredPermission, permissionImpliedBy)
              .filter { it != null }
              .joinToString(System.lineSeparator())
            throw GradleException(errorMessage)
          }
          integration.downloadDirectory
            .file("plugins.json")
            .get()
            .asFile
            .writeBytes(response.body()!!.bytes())
        }
      }
    }
  }

  private fun setupRetrieveJenkinsVersion(integration: JenkinsIntegrationExtension, task: Task) {
    task.apply {
      description = "Retrieves the version from the Jenkins instance"
      inputs.property("url", integration.baseUrl)
      outputs.file(integration.downloadDirectory.file("core-version.txt"))
      outputs.upToDateWhen { false }
      doLast {
        connect(HttpUrl.get(
          integration.baseUrl.get())!!,
          integration.authentication.getOrElse(AnonymousAuthentication)
        ).use { response ->
          val version = response.header("X-Jenkins") ?: throw GradleException("Could not retrieve Jenkins version ${response.statusLineAsMessage}")
          integration.downloadDirectory
            .file("core-version.txt")
            .get()
            .asFile
            .writeText(version)
        }
      }
    }
  }

  private val Response.statusLineAsMessage: String get() = "(Response: ${code()} ${message()})"

  private val Headers.xPermissionImpliedBy: List<String>
    get() = values("X-Permission-Implied-By")

  private val Headers.xRequiredPermission: String?
   get() = get("X-Required-Permission")

  private val Headers.xYouAreAuthenticatedAs: String?
    get() = get("X-You-Are-Authenticated-As")
}
