package com.mkobit.jenkins.pipelines

import com.mkobit.jenkins.pipelines.http.AnonymousAuthentication
import com.mkobit.jenkins.pipelines.http.BasicAuthentication
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
    assertThat(extension)
      .isNotNull()
      .isInstanceOf(JenkinsIntegrationExtension::class.java)
  }

  @Test
  internal fun `integration extension has default values`() {
    val extension = project.extensions.findByType(JenkinsIntegrationExtension::class.java)
    assertThat(extension).isNotNull()
    assertThat(extension!!).satisfies {
      assertThat(it.instanceUri)
        .describedAs("Instance URI is null")
        .isNull()
      assertThat(it.authentication.get())
        .describedAs("Anonymous authentication are the default")
        .isSameAs(AnonymousAuthentication)
    }
  }

  @Test
  internal fun `can specify alternate credential providers in extension`() {
    val basicAuth = BasicAuthentication("username", "password")
    val extension = project.extensions.findByType(JenkinsIntegrationExtension::class.java)
    assertThat(extension).isNotNull()
    extension!!.authentication.set(project.provider { basicAuth })
    assertThat(extension.authentication.get()).isEqualTo(basicAuth)
  }
}
