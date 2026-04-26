package com.mkobit.jenkins.pipelines

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.gradle.testkit.runner.TaskOutcome
import testsupport.TestProjectBuilder
import testsupport.TestedGradleVersion

class JenkinsIntegrationPluginFunctionalTest :
  DescribeSpec({
    fun integrationProject(
      serverUrl: String,
      authBlock: String = "",
    ): TestProjectBuilder =
      TestProjectBuilder().apply {
        buildFile.writeText(
          """
          plugins {
              id("com.mkobit.jenkins.pipelines.jenkins-integration")
          }
          jenkinsIntegration {
              baseUrl.set(java.net.URL("$serverUrl"))
              $authBlock
          }
          """.trimIndent(),
        )
      }

    describe("retrieveJenkinsGdsl") {
      describe("downloads GDSL when Jenkins responds with 200") {
        withData(TestedGradleVersion.entries) { gradleVersion ->
          val server = MockWebServer()
          server.enqueue(MockResponse().setBody("// gdsl content"))
          server.start()
          try {
            integrationProject(server.url("/").toString()).use { project ->
              val result =
                project
                  .runner(gradleVersion)
                  .withArguments("retrieveJenkinsGdsl")
                  .build()
              result.task(":retrieveJenkinsGdsl")!!.outcome shouldBe TaskOutcome.SUCCESS
              project.file("build/jenkinsIntegrationDownloads/idea.gdsl").readText() shouldBe "// gdsl content"
            }
          } finally {
            server.shutdown()
          }
        }
      }

      describe("fails build when Jenkins returns 500") {
        withData(TestedGradleVersion.entries) { gradleVersion ->
          val server = MockWebServer()
          server.enqueue(MockResponse().setResponseCode(500))
          server.start()
          try {
            integrationProject(server.url("/").toString()).use { project ->
              val result =
                project
                  .runner(gradleVersion)
                  .withArguments("retrieveJenkinsGdsl")
                  .buildAndFail()
              result.task(":retrieveJenkinsGdsl")!!.outcome shouldBe TaskOutcome.FAILED
              result.output shouldContain "Error retrieving GDSL"
            }
          } finally {
            server.shutdown()
          }
        }
      }
    }

    describe("retrieveJenkinsPluginData") {
      describe("downloads plugins.json when Jenkins responds with 200") {
        withData(TestedGradleVersion.entries) { gradleVersion ->
          val server = MockWebServer()
          server.enqueue(MockResponse().setBody("""{"plugins":[]}"""))
          server.start()
          try {
            integrationProject(server.url("/").toString()).use { project ->
              val result =
                project
                  .runner(gradleVersion)
                  .withArguments("retrieveJenkinsPluginData")
                  .build()
              result.task(":retrieveJenkinsPluginData")!!.outcome shouldBe TaskOutcome.SUCCESS
              project.file("build/jenkinsIntegrationDownloads/plugins.json").readText() shouldBe """{"plugins":[]}"""
            }
          } finally {
            server.shutdown()
          }
        }
      }

      describe("reports unauthorized error when Jenkins responds with 403") {
        withData(TestedGradleVersion.entries) { gradleVersion ->
          val server = MockWebServer()
          server.enqueue(
            MockResponse()
              .setResponseCode(403)
              .addHeader("X-You-Are-Authenticated-As", "mkobit")
              .addHeader("X-Required-Permission", "hudson.model.Hudson.Read")
              .addHeader("X-Permission-Implied-By", "hudson.model.Hudson.Administer"),
          )
          server.start()
          try {
            integrationProject(server.url("/").toString()).use { project ->
              val result =
                project
                  .runner(gradleVersion)
                  .withArguments("retrieveJenkinsPluginData")
                  .buildAndFail()
              result.task(":retrieveJenkinsPluginData")!!.outcome shouldBe TaskOutcome.FAILED
              result.output shouldContain "Unauthorized to retrieve plugin data"
              result.output shouldContain "User: mkobit"
              result.output shouldContain "Permission required: hudson.model.Hudson.Read"
              result.output shouldContain "Permission implied by: hudson.model.Hudson.Administer"
            }
          } finally {
            server.shutdown()
          }
        }
      }

      describe("reports invalid credentials error when Jenkins responds with 401") {
        withData(TestedGradleVersion.entries) { gradleVersion ->
          val server = MockWebServer()
          server.enqueue(MockResponse().setResponseCode(401).setBody("<html>Invalid password/token</html>"))
          server.start()
          try {
            integrationProject(
              server.url("/").toString(),
              authBlock =
                """authentication.set(
                  com.mkobit.jenkins.pipelines.http.BasicAuthentication("mkobit", "hunter2")
                )""",
            ).use { project ->
              val result =
                project
                  .runner(gradleVersion)
                  .withArguments("retrieveJenkinsPluginData")
                  .buildAndFail()
              result.task(":retrieveJenkinsPluginData")!!.outcome shouldBe TaskOutcome.FAILED
              result.output shouldContain "Unable to authenticate due to invalid password/token"
              result.output shouldNotContain "hunter2"
            }
          } finally {
            server.shutdown()
          }
        }
      }
    }

    describe("retrieveJenkinsVersion") {
      describe("writes version to file when Jenkins responds with X-Jenkins header") {
        withData(TestedGradleVersion.entries) { gradleVersion ->
          val server = MockWebServer()
          server.enqueue(
            MockResponse()
              .setResponseCode(200)
              .addHeader("X-Jenkins", "2.479.1"),
          )
          server.start()
          try {
            integrationProject(server.url("/").toString()).use { project ->
              val result =
                project
                  .runner(gradleVersion)
                  .withArguments("retrieveJenkinsVersion")
                  .build()
              result.task(":retrieveJenkinsVersion")!!.outcome shouldBe TaskOutcome.SUCCESS
              project.file("build/jenkinsIntegrationDownloads/core-version.txt").readText() shouldBe "2.479.1"
            }
          } finally {
            server.shutdown()
          }
        }
      }

      describe("fails build when response has no X-Jenkins header") {
        withData(TestedGradleVersion.entries) { gradleVersion ->
          val server = MockWebServer()
          server.enqueue(MockResponse().setResponseCode(200))
          server.start()
          try {
            integrationProject(server.url("/").toString()).use { project ->
              val result =
                project
                  .runner(gradleVersion)
                  .withArguments("retrieveJenkinsVersion")
                  .buildAndFail()
              result.task(":retrieveJenkinsVersion")!!.outcome shouldBe TaskOutcome.FAILED
              result.output shouldContain "Could not retrieve Jenkins version"
            }
          } finally {
            server.shutdown()
          }
        }
      }
    }
  })
