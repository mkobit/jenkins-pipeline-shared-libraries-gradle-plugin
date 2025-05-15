package com.mkobit.jenkins.pipelines

import com.mkobit.jenkins.pipelines.http.AnonymousAuthentication
import com.mkobit.jenkins.pipelines.http.BasicAuthentication
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.apply
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNullOrBlank
import testsupport.minutest.testFactory
import testsupport.strikt.value

internal class JenkinsIntegrationPluginTest {

  private lateinit var project: Project

  @BeforeEach
  internal fun setUp() {
    project = ProjectBuilder.builder().build()
    project.apply<JenkinsIntegrationPlugin>()
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
        get("Instance URL is absent") { baseUrl.isPresent }.isFalse()
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

  @TestFactory
  internal fun `plugin tasks`() = testFactory<Project> {
    fixture {
      val project = ProjectBuilder.builder().build()
      project.apply<JenkinsIntegrationPlugin>()
      project
    }

    derivedContext<TaskContainer>("task") {
      deriveFixture { tasks }
      listOf(
        "retrieveJenkinsGdsl",
        "retrieveJenkinsVersion",
        "retrieveJenkinsPluginData"
      ).forEach { name ->
        derivedContext<Task?>("with name $name") {
          deriveFixture { findByName(name) }
          test("exists") {
            expectThat(fixture).isNotNull()
          }

          test("group is not blank") {
            expectThat(fixture).isNotNull().get { group }.not { isNullOrBlank() }
          }
        }
      }
    }
  }
}
