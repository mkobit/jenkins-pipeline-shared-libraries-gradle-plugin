package com.mkobit.jenkins.pipelines

import com.mkobit.jenkins.pipelines.auth.AnonymousCredentials
import com.mkobit.jenkins.pipelines.auth.UsernamePasswordCredentials
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import testsupport.NotImplementedYet

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
      assertThat(it.credentials.get())
        .describedAs("Anonymous credentials are the default")
        .isSameAs(AnonymousCredentials)
    }
  }

  @Test
  internal fun `can specify alternate credential providers in extension`() {
    val basicAuth = UsernamePasswordCredentials("username", "password")
    val extension = project.extensions.findByType(JenkinsIntegrationExtension::class.java)
    assertThat(extension).isNotNull()
    extension!!.credentials.set(project.provider { basicAuth })
    assertThat(extension.credentials.get()).isEqualTo(basicAuth)
  }
}
