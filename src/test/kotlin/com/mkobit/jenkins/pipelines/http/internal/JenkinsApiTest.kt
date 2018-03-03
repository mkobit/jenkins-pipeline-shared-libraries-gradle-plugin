package com.mkobit.jenkins.pipelines.http.internal

import com.mkobit.jenkins.pipelines.http.AnonymousAuthentication
import com.mkobit.jenkins.pipelines.http.ApiTokenAuthentication
import com.mkobit.jenkins.pipelines.http.BasicAuthentication
import okhttp3.Credentials
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import testsupport.UseMockServer

@UseMockServer
internal class JenkinsApiTest {

  @Test
  internal fun GDSL(server: MockWebServer) {
    server.enqueue(MockResponse())
    server.start()

    downloadGdsl(server.url("jenkins"), AnonymousAuthentication)
    val request = server.takeRequest()
    assertThat(request.method).isEqualTo("GET")
    assertThat(request.path).endsWith("pipeline-syntax/gdsl")
  }

  @Test
  internal fun `plugin manager plugins`(server: MockWebServer) {
    server.enqueue(MockResponse())
    server.start()

    retrievePluginManagerData(server.url("jenkins"), AnonymousAuthentication)
    val request = server.takeRequest()
    assertThat(request.method).isEqualTo("GET")
    assertThat(request.requestUrl).satisfies {
      assertThat(it.pathSegments()).containsExactly("jenkins", "pluginManager", "api", "json")
      assertThat(it.queryParameter("depth")).isEqualTo("2")
    }
  }

  @Test
  internal fun `head request against base URL`(server: MockWebServer) {
    server.enqueue(MockResponse())
    server.start()

    connect(server.url("jenkins"), AnonymousAuthentication)
    val request = server.takeRequest()
    assertThat(request.requestUrl.pathSegments()).containsExactly("jenkins")
    assertThat(request.method).isEqualTo("HEAD")
  }

  @Test
  internal fun `basic authentication headers`(server: MockWebServer) {
    server.enqueue(MockResponse())
    server.start()

    val basic = BasicAuthentication("mkobit", "hunter2")

    retrievePluginManagerData(server.url("jenkins"), basic)
    val request = server.takeRequest()
    assertThat(request.headers).satisfies {
      assertThat(it["Authentication"]).isEqualTo(Credentials.basic(basic.username, basic.password))
    }
  }

  @Test
  internal fun `token authentication headers`(server: MockWebServer) {
    server.enqueue(MockResponse())
    server.start()

    val token = ApiTokenAuthentication("mkobit", "0123456789abcdef")

    retrievePluginManagerData(server.url("jenkins"), token)
    val request = server.takeRequest()
    assertThat(request.headers).satisfies {
      assertThat(it["Authentication"]).isEqualTo(Credentials.basic(token.username, token.apiToken))
    }
  }
}
