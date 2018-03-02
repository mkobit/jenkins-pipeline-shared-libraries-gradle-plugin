package com.mkobit.jenkins.pipelines

import com.mkobit.jenkins.pipelines.http.AnonymousAuthentication
import com.mkobit.jenkins.pipelines.http.Authentication
import com.mkobit.jenkins.pipelines.http.internal.downloadGdsl
import com.mkobit.jenkins.pipelines.http.internal.retrievePluginManagerData
import mu.KotlinLogging
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.Response
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
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
      val integrationExtension = extensions.create(
        EXTENSION_NAME,
        JenkinsIntegrationExtension::class.java,
        objects.property<URL>(),
        objects.property<Authentication>().apply { set(AnonymousAuthentication) },
        layout.directoryProperty(layout.buildDirectory.dir("jenkinsIntegrationDownloads"))
      )

      tasks {
        "retrieveJenkinsGdsl" {
          description = "Downloads the Jenkins Pipeline GDSL from the Jenkins instance"
          inputs.property("url", integrationExtension.baseUrl)
          outputs.file(integrationExtension.downloadDirectory.file("idea.gdsl"))
          outputs.upToDateWhen { false }
          doLast {
            val response = downloadGdsl(HttpUrl.get(
              integrationExtension.baseUrl.get())!!,
              integrationExtension.authentication.getOrElse(AnonymousAuthentication)
            )
            if (!response.isSuccessful) throw GradleException("Error retrieving GDSL. ${response.statusLineAsMessage}")
            integrationExtension.downloadDirectory.file("idea.gdsl").get().asFile.writeBytes(response.body()!!.bytes())
          }
        }

        "retrieveJenkinsPluginData" {
          description = "Downloads the Jenkins plugin data from the Jenkins instance"
          inputs.property("url", integrationExtension.baseUrl)
          outputs.file(integrationExtension.downloadDirectory.file("plugins.json"))
          outputs.upToDateWhen { false }
          doLast {
            val response = retrievePluginManagerData(HttpUrl.get(
              integrationExtension.baseUrl.get())!!,
              integrationExtension.authentication.getOrElse(AnonymousAuthentication)
            )
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
                else ->"Error downloading plugin data ${response.statusLineAsMessage}"
              }
              val errorMessage = listOf(errorSubject, userDetails, requiredPermission, permissionImpliedBy)
                .filter { it != null }
                .joinToString(System.lineSeparator())
              throw GradleException(errorMessage)
            }
            integrationExtension.downloadDirectory.file("plugins.json").get().asFile.writeBytes(response.body()!!.bytes())
          }
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
