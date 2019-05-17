package com.mkobit.jenkins.pipelines

import com.mkobit.jenkins.pipelines.http.AnonymousAuthentication
import com.mkobit.jenkins.pipelines.http.BasicAuthentication
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import testsupport.expectDoesNotThrow
import testsupport.strikt.isPresent
import testsupport.strikt.value

internal class JenkinsIntegrationPluginTest {

  private lateinit var project: Project

  @BeforeEach
  internal fun setUp() {
    project = ProjectBuilder.builder().build()
    project.pluginManager.apply(JenkinsIntegrationPlugin::class.java)
  }

  @Test
  internal fun `jenkinsIntegration extension is created`() {
    val extension = project.extensions.findByName("jenkinsIntegration")
    expectThat(extension)
      .isNotNull()
      .isA<JenkinsIntegrationExtension>()
  }

  @Test
  internal fun `integration extension has default values`() {
    val extension = project.extensions.findByType(JenkinsIntegrationExtension::class.java)
    expectThat(extension)
      .isNotNull()
      .and {
        get("Instance URL is absent") { baseUrl }.not { isPresent() }
        get("Anonymous authentication is the default") { authentication }
          .value
          .isEqualTo(AnonymousAuthentication)
      }
  }

  @Test
  internal fun `can specify alternate credential providers in extension`() {
    val basicAuth = BasicAuthentication("username", "password")
    val extension = project.extensions.findByType(JenkinsIntegrationExtension::class.java)?.apply {
      authentication.set(project.provider { basicAuth })
    }
    expectThat(extension)
      .isNotNull()
      .and {
        get { authentication }
          .value
          .isEqualTo(basicAuth)
      }
  }

  @Test
  internal fun `download GDSL task exists`() {
    expectDoesNotThrow {
      project.tasks.named("retrieveJenkinsGdsl")
    }
  }
}
