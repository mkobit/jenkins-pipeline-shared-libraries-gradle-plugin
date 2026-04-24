package com.mkobit.jenkins.pipelines

import com.mkobit.jenkins.pipelines.http.AnonymousAuthentication
import com.mkobit.jenkins.pipelines.http.BasicAuthentication
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.types.shouldBeInstanceOf
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

internal class JenkinsIntegrationPluginTest {
  private lateinit var project: Project

  @BeforeEach
  internal fun setUp() {
    project = ProjectBuilder.builder().build()
    project.apply<JenkinsIntegrationPlugin>()
  }

  @Test
  internal fun `jenkinsIntegration extension is created`() {
    project.extensions.findByName("jenkinsIntegration")
      .shouldNotBeNull()
      .shouldBeInstanceOf<JenkinsIntegrationExtension>()
  }

  @Test
  internal fun `integration extension has default values`() {
    val extension = project.extensions.findByType(JenkinsIntegrationExtension::class.java).shouldNotBeNull()
    extension.baseUrl.isPresent shouldBe false
    extension.authentication.get() shouldBe AnonymousAuthentication
  }

  @Test
  internal fun `can specify alternate credential providers in extension`() {
    val basicAuth = BasicAuthentication("username", "password")
    val extension = project.extensions.findByType(JenkinsIntegrationExtension::class.java).shouldNotBeNull()
    extension.authentication.set(project.provider { basicAuth })
    extension.authentication.get() shouldBe basicAuth
  }

  @TestFactory
  internal fun `plugin tasks`(): List<DynamicTest> =
    listOf("retrieveJenkinsGdsl", "retrieveJenkinsVersion", "retrieveJenkinsPluginData").flatMap { name ->
      listOf(
        dynamicTest("task '$name' exists") {
          project.tasks.findByName(name).shouldNotBeNull()
        },
        dynamicTest("task '$name' group is not blank") {
          project.tasks.findByName(name)?.group.shouldNotBeBlank()
        },
      )
    }
}
