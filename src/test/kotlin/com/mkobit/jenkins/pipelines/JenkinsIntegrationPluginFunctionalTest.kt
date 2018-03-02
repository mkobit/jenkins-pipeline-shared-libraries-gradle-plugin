package com.mkobit.jenkins.pipelines

import com.mkobit.gradle.test.kotlin.io.Original
import com.mkobit.gradle.test.kotlin.testkit.runner.build
import com.mkobit.gradle.test.kotlin.testkit.runner.buildAndFail
import com.mkobit.gradle.test.kotlin.testkit.runner.setupProjectDir
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.TestTemplate
import testsupport.ForGradleVersions
import testsupport.GradleProject
import testsupport.NotImplementedYet
import testsupport.UseMockServer
import testsupport.loadResource
import java.nio.file.Paths

@UseMockServer
@ForGradleVersions(["current"])
internal class JenkinsIntegrationPluginFunctionalTest {

  companion object {
    private val downloadDirectory = Paths.get("build", "jenkinsFunctionalTest")
  }

  @TestTemplate
  internal fun `can retrieve the GDSL`(@GradleProject(["projects", "only-plugins-block"]) gradleRunner: GradleRunner, server: MockWebServer) {
    server.enqueue(MockResponse().setBody(loadResource("jenkins-data/http/200.gdsl.txt")))
    server.start()

    gradleRunner.setupBaseUrl(server)

    val result = gradleRunner.build("retrieveJenkinsGdsl")

    assertThat(result.projectDir.resolve(downloadDirectory.resolve("idea.gdsl")))
      .isRegularFile()
  }

  @TestTemplate
  internal fun `build fails when the GDSL cannot be retrieved`(@GradleProject(["projects", "only-plugins-block"]) gradleRunner: GradleRunner, server: MockWebServer) {
    server.enqueue(MockResponse().setResponseCode(400))
    server.start()

    gradleRunner.setupBaseUrl(server)

    gradleRunner.buildAndFail("retrieveJenkinsGdsl")
  }

  @NotImplementedYet
  @TestTemplate
  internal fun `can retrieve the global security whitelist`(@GradleProject gradleRunner: GradleRunner) {
  }

  @NotImplementedYet
  @TestTemplate
  internal fun `a useful error message is displayed when user has insignificant privileges to retrieve the global security whitelist`(@GradleProject gradleRunner: GradleRunner) {
  }

  @NotImplementedYet
  @TestTemplate
  internal fun `can retrieve the list of plugins`(@GradleProject gradleRunner: GradleRunner) {
  }

  @NotImplementedYet
  @TestTemplate
  internal fun `a useful error message is displayed when user has insignificant privileges to retrieve the list of plugins`(@GradleProject gradleRunner: GradleRunner) {
  }

  @NotImplementedYet
  @TestTemplate
  internal fun `can retrieve the Jenkins version`(@GradleProject gradleRunner: GradleRunner) {
  }

  @NotImplementedYet
  @TestTemplate
  internal fun `can retrieve the list of global library configurations`(@GradleProject gradleRunner: GradleRunner) {
  }

  @NotImplementedYet
  @TestTemplate
  internal fun `a useful error message is displayed when user has insignificant privileges to retrieve the global library configurations`(@GradleProject gradleRunner: GradleRunner) {
  }

  private fun GradleRunner.setupBaseUrl(server: MockWebServer) {
    setupProjectDir {
      "build.gradle"(content = Original) {

      }
      file("build.gradle") {
        appendNewline()
        append("""
          jenkinsIntegration {
            baseUrl = new java.net.URL('${server.url("jenkins").url()}')
            downloadDirectory = layout.buildDirectory.dir('${downloadDirectory.fileName}')
          }
        """.trimIndent())
      }
    }
  }
}
