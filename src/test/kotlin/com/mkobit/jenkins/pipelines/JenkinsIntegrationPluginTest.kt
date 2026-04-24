package com.mkobit.jenkins.pipelines

import com.mkobit.jenkins.pipelines.http.AnonymousAuthentication
import com.mkobit.jenkins.pipelines.http.BasicAuthentication
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.types.shouldBeInstanceOf
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.testfixtures.ProjectBuilder

internal class JenkinsIntegrationPluginTest : DescribeSpec({
  lateinit var project: Project

  beforeTest {
    project = ProjectBuilder.builder().build()
    project.apply<JenkinsIntegrationPlugin>()
  }

  describe("jenkinsIntegration extension") {
    it("is registered") {
      project.extensions.findByName("jenkinsIntegration")
        .shouldNotBeNull()
        .shouldBeInstanceOf<JenkinsIntegrationExtension>()
    }

    it("has no base URL by default") {
      val ext = project.extensions.findByType(JenkinsIntegrationExtension::class.java).shouldNotBeNull()
      ext.baseUrl.isPresent shouldBe false
    }

    it("uses anonymous authentication by default") {
      val ext = project.extensions.findByType(JenkinsIntegrationExtension::class.java).shouldNotBeNull()
      ext.authentication.get() shouldBe AnonymousAuthentication
    }

    it("accepts an alternate authentication provider") {
      val basicAuth = BasicAuthentication("username", "password")
      val ext = project.extensions.findByType(JenkinsIntegrationExtension::class.java).shouldNotBeNull()
      ext.authentication.set(project.provider { basicAuth })
      ext.authentication.get() shouldBe basicAuth
    }
  }

  listOf("retrieveJenkinsGdsl", "retrieveJenkinsVersion", "retrieveJenkinsPluginData").forEach { taskName ->
    describe("task '$taskName'") {
      it("is registered") {
        project.tasks.findByName(taskName).shouldNotBeNull()
      }

      it("has a non-blank group") {
        project.tasks.findByName(taskName)?.group.shouldNotBeBlank()
      }
    }
  }
})
