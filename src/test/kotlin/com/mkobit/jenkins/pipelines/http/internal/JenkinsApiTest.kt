package com.mkobit.jenkins.pipelines.http.internal

import com.mkobit.jenkins.pipelines.http.AnonymousAuthentication
import com.mkobit.jenkins.pipelines.http.ApiTokenAuthentication
import com.mkobit.jenkins.pipelines.http.BasicAuthentication
import okhttp3.Credentials
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.endsWith
import strikt.assertions.isEqualTo
import testsupport.junit.UseMockServer

@UseMockServer
internal class JenkinsApiTest {

  @Test
  internal fun `retrieve GDSL`(server: MockWebServer) {
    server.enqueue(MockResponse())
    server.start()

    downloadGdsl(server.url("jenkins"), AnonymousAuthentication)
    val request = server.takeRequest()
    expectThat(request.method).isEqualTo("GET")
    expectThat(request.path).endsWith("pipeline-syntax/gdsl")
  }

  @Test
  internal fun `plugin manager plugins`(server: MockWebServer) {
    server.enqueue(MockResponse())
    server.start()

    retrievePluginManagerData(server.url("jenkins"), AnonymousAuthentication)
    val request = server.takeRequest()
    expectThat(request) {
      get { method }.isEqualTo("GET")
      get { requestUrl }.and {
        get { pathSegments() }.containsExactly("jenkins", "pluginManager", "api", "json")
        get("query parameter depth") { queryParameter("depth") }.isEqualTo("2")
      }
    }
  }

  @Test
  internal fun `head request against base URL`(server: MockWebServer) {
    server.enqueue(MockResponse())
    server.start()

    connect(server.url("jenkins"), AnonymousAuthentication)
    val request = server.takeRequest()
    expectThat(request) {
      get { method }.isEqualTo("HEAD")
      get { requestUrl }.and {
        get { pathSegments() }.containsExactly("jenkins")
      }
    }
  }

  @Test
  internal fun `basic authentication headers`(server: MockWebServer) {
    server.enqueue(MockResponse())
    server.start()

    val basic = BasicAuthentication("mkobit", "hunter2")

    retrievePluginManagerData(server.url("jenkins"), basic)
    val request = server.takeRequest()
    expectThat(request) {
      get { headers }.and {
        get { get("Authorization") }.isEqualTo(Credentials.basic(basic.username, basic.password))
      }
    }
  }

  @Test
  internal fun `token authentication headers`(server: MockWebServer) {
    server.enqueue(MockResponse())
    server.start()

    val token = ApiTokenAuthentication("mkobit", "0123456789abcdef")

    retrievePluginManagerData(server.url("jenkins"), token)
    val request = server.takeRequest()
    expectThat(request) {
      get { headers }.and {
        get { get("Authorization") }.isEqualTo(Credentials.basic(token.username, token.apiToken))
      }
    }
  }
}
