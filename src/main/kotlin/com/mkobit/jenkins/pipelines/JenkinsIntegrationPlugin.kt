package com.mkobit.jenkins.pipelines

import com.mkobit.jenkins.pipelines.http.AnonymousAuthentication
import com.mkobit.jenkins.pipelines.http.Authentication
import com.mkobit.jenkins.pipelines.http.internal.downloadGdsl
import mu.KotlinLogging
import okhttp3.HttpUrl
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
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

      tasks.create("downloadGdslFromJenkins") {
        description = "Downloads the Jenkins Pipeline from th instance GDSL"
        inputs.property("url", integrationExtension.baseUrl)
        outputs.file(integrationExtension.downloadDirectory.file("idea.gdsl"))
        outputs.upToDateWhen { false }
        doLast {
          val response = downloadGdsl(HttpUrl.get(
            integrationExtension.baseUrl.get())!!,
            integrationExtension.authentication.getOrElse(AnonymousAuthentication)
          )
          if (!response.isSuccessful) throw GradleException("Error code ${response.code()} downloading GDSL")
          integrationExtension.downloadDirectory.file("idea.gdsl").get().asFile.writeBytes(response.body()!!.bytes())
        }
      }
    }
  }
}
