package com.mkobit.jenkins.pipelines

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

  @NotImplementedYet
  @Test
  internal fun `jenkinsIntegration is created`() {
    val extension = project.extensions.findByName("jenkinsIntegration")
    assertThat(extension)
      .isNotNull()
      .isInstanceOf(JenkinsIntegrationExtension::class.java)
  }

  @NotImplementedYet
  @Test
  internal fun `can specify alternate credential providers in extension`() {
  }
}
