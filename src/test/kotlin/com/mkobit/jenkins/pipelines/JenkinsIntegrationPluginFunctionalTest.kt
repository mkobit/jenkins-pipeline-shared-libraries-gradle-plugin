package com.mkobit.jenkins.pipelines

import com.mkobit.gradle.test.kotlin.io.Original
import com.mkobit.gradle.test.kotlin.testkit.runner.build
import com.mkobit.gradle.test.kotlin.testkit.runner.buildAndFail
import com.mkobit.gradle.test.kotlin.testkit.runner.setupProjectDir
import com.mkobit.jenkins.pipelines.http.AnonymousAuthentication
import com.mkobit.jenkins.pipelines.http.ApiTokenAuthentication
import com.mkobit.jenkins.pipelines.http.Authentication
import com.mkobit.jenkins.pipelines.http.BasicAuthentication
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestTemplate
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.gradle.testkit.output
import strikt.java.allBytes
import strikt.java.isRegularFile
import strikt.java.resolve
import testsupport.io.loadResource
import testsupport.junit.ForGradleVersions
import testsupport.junit.GradleProject
import testsupport.junit.NotImplementedYet
import testsupport.junit.UseMockServer
import testsupport.strikt.allOf
import java.nio.file.Paths

@UseMockServer
@ForGradleVersions(["current"])
internal class JenkinsIntegrationPluginFunctionalTest {

  companion object {
    // The path to where download files are saved.
    private val downloadDirectory = Paths.get("build", "jenkinsFunctionalTest")
  }

  @TestTemplate
  internal fun `can retrieve the GDSL`(
    @GradleProject(["projects", "only-plugins-block"]) gradleRunner: GradleRunner,
    server: MockWebServer
  ) {
    val content = loadResource("jenkins-data/http/200.gdsl.txt")
    server.enqueue(MockResponse().setBody(content))
    server.start()

    gradleRunner.setupIntegrationExtension(server)

    expectThat(gradleRunner.build("retrieveJenkinsGdsl"))
      .get { projectDir }
      .resolve(downloadDirectory)
      .resolve("idea.gdsl")
      .isRegularFile()
      .allBytes()
      .get { toString(Charsets.UTF_8) }
      .isEqualTo(content)
  }

  @TestTemplate
  internal fun `failing to retrieve GDSL results in failed build`(
    @GradleProject(["projects", "only-plugins-block"]) gradleRunner: GradleRunner,
    server: MockWebServer
  ) {
    server.enqueue(MockResponse().setResponseCode(500))
    server.start()

    gradleRunner.setupIntegrationExtension(server)

    expectThat(gradleRunner.buildAndFail("retrieveJenkinsGdsl"))
      .output
      .contains("Error retrieving GDSL")
      .contains("(Response: 500 Server Error)")
  }

  @NotImplementedYet
  @TestTemplate
  internal fun `can retrieve the global security whitelist`(@GradleProject gradleRunner: GradleRunner) {
  }

  @NotImplementedYet
  @Test
  internal fun `a useful error message is displayed when invalid authentication is used to retrieve the global security whitelist`(
    @GradleProject(
      ["projects", "only-plugins-block"]
    ) gradleRunner: GradleRunner,
    server: MockWebServer
  ) {
  }

  @NotImplementedYet
  @TestTemplate
  internal fun `a useful error message is displayed when user has insufficient privileges to retrieve the global security whitelist`(
    @GradleProject(
      ["projects", "only-plugins-block"]
    ) gradleRunner: GradleRunner,
    server: MockWebServer
  ) {
  }

  @TestTemplate
  internal fun `can retrieve the list of plugins`(
    @GradleProject(["projects", "only-plugins-block"]) gradleRunner: GradleRunner,
    server: MockWebServer
  ) {
    val content = loadResource("jenkins-data/http/200.pluginManager.json")
    server.enqueue(MockResponse().setBody(content))
    server.start()

    gradleRunner.setupIntegrationExtension(server)

    expectThat(gradleRunner.build("retrieveJenkinsPluginData"))
      .get { projectDir }
      .resolve(downloadDirectory)
      .resolve("plugins.json")
      .isRegularFile()
      .allBytes()
      .get { toString(Charsets.UTF_8) }
      .isEqualTo(content)
  }

  @TestTemplate
  internal fun `a useful error message is displayed when user has insufficient privileges to retrieve the list of plugins`(
    @GradleProject(
      ["projects", "only-plugins-block"]
    ) gradleRunner: GradleRunner,
    server: MockWebServer
  ) {
    server.enqueue(
      MockResponse().apply {
        setResponseCode(403)
        setHeader("X-You-Are-Authenticated-As", "mkobit")
        setHeader("X-Required-Permission", "hudson.model.Hudson.Read")
        addHeader("X-Permission-Implied-By", "hudson.security.Permission.GenericRead")
        addHeader("X-Permission-Implied-By", "hudson.model.Hudson.Administer")
        setBody(
          """
        <html><head><meta http-equiv='refresh' content='1;url=/login?from=%2FpluginManager%2Fapi%2Fjson%3Fdepth%3D2'/><script>window.location.replace('/login?from=%2FpluginManager%2Fapi%2Fjson%3Fdepth%3D2');</script></head><body style='background-color:white; color:white;'>


        Authentication required
        <!--
        You are authenticated as: mkobit
        Groups that you are in:

        Permission you need to have (but didn't): hudson.model.Hudson.Read
         ... which is implied by: hudson.security.Permission.GenericRead
         ... which is implied by: hudson.model.Hudson.Administer
        -->

        </body></html>
          """.trimIndent()
        )
      }
    )
    server.start()

    gradleRunner.setupIntegrationExtension(server, BasicAuthentication("mkobit", "hunter2"))

    expectThat(gradleRunner.buildAndFail("retrieveJenkinsPluginData"))
      .output
      .and {
        allOf {
          contains("Unauthorized to retrieve plugin data")
          contains("(Response: 403 Client Error)")
          contains("User: mkobit")
          contains("Permission required: hudson.model.Hudson.Read")
          contains("Permission implied by: hudson.model.Hudson.Administer")
          contains("Permission implied by: hudson.security.Permission.GenericRead")
        }
      }
  }

  @TestTemplate
  internal fun `a useful error message is displayed when invalid authentication is used to retrieve the list of plugins`(
    @GradleProject(
      ["projects", "only-plugins-block"]
    ) gradleRunner: GradleRunner,
    server: MockWebServer
  ) {
    server.enqueue(
      MockResponse().apply {
        setResponseCode(401)
        setBody(
          """
          <html>
          <head>
          <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>
          <title>Error 401 Invalid password/token for user: mkobit</title>
          </head>
          <body><h2>HTTP ERROR 401</h2>
          <p>Problem accessing /pluginManager/api/json. Reason:
          <pre>    Invalid password/token for user: mkobit</pre></p><hr><a href="http://eclipse.org/jetty">Powered by Jetty:// 9.4.z-SNAPSHOT</a><hr/>
          """.trimIndent()
        )
      }
    )
    server.start()

    gradleRunner.setupIntegrationExtension(server, BasicAuthentication("mkobit", "hunter2"))

    expectThat(gradleRunner.buildAndFail("retrieveJenkinsPluginData"))
      .output
      .contains("Unable to authenticate due to invalid password/token")
      .contains("(Response: 401 Client Error)")
      .not { contains("hunter2") }
  }

  @TestTemplate
  internal fun `can retrieve the Jenkins version`(
    @GradleProject(["projects", "only-plugins-block"]) gradleRunner: GradleRunner,
    server: MockWebServer
  ) {
    val version = "2.89.4"
    server.enqueue(
      MockResponse().apply {
        setResponseCode(401)
        setHeader("X-Hudson", "1.395")
        setHeader("X-Jenkins", version)
        setHeader("X-Jenkins-Session", "4d661237")
        setHeader("X-Hudson-CLI-Port", "40877")
        setHeader("X-Jenkins-CLI-Port", "40877")
        setHeader("X-Jenkins-CLI2-Port", "40877")
        setHeader("X-You-Are-Authenticated-As", "anonymous")
      }
    )
    server.start()

    gradleRunner.setupIntegrationExtension(server, BasicAuthentication("mkobit", "hunter2"))

    expectThat(gradleRunner.build("retrieveJenkinsVersion"))
      .get { projectDir }
      .resolve(downloadDirectory)
      .resolve("core-version.txt")
      .isRegularFile()
      .allBytes()
      .get { toString(Charsets.UTF_8) }
      .isEqualTo(version)
  }

  @NotImplementedYet
  @TestTemplate
  internal fun `a useful error message is displayed when the the Jenkins version cannot be retrieved`(@GradleProject gradleRunner: GradleRunner) {
  }

  @NotImplementedYet
  @TestTemplate
  internal fun `can retrieve the list of global library configurations`(@GradleProject gradleRunner: GradleRunner) {
  }

  @NotImplementedYet
  @TestTemplate
  internal fun `a useful error message is displayed when user has insufficient privileges to retrieve the global library configurations`(@GradleProject gradleRunner: GradleRunner) {
  }

  private fun GradleRunner.setupIntegrationExtension(server: MockWebServer, authentication: Authentication? = null) {
    val authenticationText = when (authentication) {
      is AnonymousAuthentication -> throw IllegalArgumentException("AnonymousAuthentication should not be tested against since it is the default")
      is BasicAuthentication -> "providers.provider { com.mkobit.jenkins.pipelines.http.BasicAuthentication('${authentication.username}', '${authentication.password}') }"
      is ApiTokenAuthentication -> "providers.provider { com.mkobit.jenkins.pipelines.http.ApiTokenAuthentication('${authentication.username}', '${authentication.apiToken}') }"
      else -> ""
    }
    setupProjectDir {
      "build.gradle"(content = Original) {
        appendNewline()
        append(
          """
          jenkinsIntegration {
            baseUrl = new java.net.URL('${server.url("jenkins").url()}')
            downloadDirectory = layout.buildDirectory.dir('${downloadDirectory.fileName}')
            $authenticationText
          }
          """.trimIndent()
        )
      }
    }
  }
}
