package com.mkobit.jenkins.pipelines.http.internal

import com.mkobit.jenkins.pipelines.http.AnonymousAuthentication
import com.mkobit.jenkins.pipelines.http.ApiTokenAuthentication
import com.mkobit.jenkins.pipelines.http.BasicAuthentication
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import okhttp3.Credentials
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import testsupport.junit.UseMockServer

@UseMockServer
internal class JenkinsApiTest {
  @Test
  internal fun `retrieve GDSL`(server: MockWebServer) {
    server.enqueue(MockResponse())
    server.start()

    downloadGdsl(server.url("jenkins"), AnonymousAuthentication)
    val request = server.takeRequest()
    request.method shouldBe "GET"
    request.path shouldEndWith "pipeline-syntax/gdsl"
  }

  @Test
  internal fun `plugin manager plugins`(server: MockWebServer) {
    server.enqueue(MockResponse())
    server.start()

    retrievePluginManagerData(server.url("jenkins"), AnonymousAuthentication)
    val request = server.takeRequest()
    request.method shouldBe "GET"
    request.requestUrl!!.pathSegments shouldBe listOf("jenkins", "pluginManager", "api", "json")
    request.requestUrl!!.queryParameter("depth") shouldBe "2"
  }

  @Test
  internal fun `head request against base URL`(server: MockWebServer) {
    server.enqueue(MockResponse())
    server.start()

    connect(server.url("jenkins"), AnonymousAuthentication)
    val request = server.takeRequest()
    request.method shouldBe "HEAD"
    request.requestUrl!!.pathSegments shouldBe listOf("jenkins")
  }

  @Test
  internal fun `basic authentication headers`(server: MockWebServer) {
    server.enqueue(MockResponse())
    server.start()

    val basic = BasicAuthentication("mkobit", "hunter2")
    retrievePluginManagerData(server.url("jenkins"), basic)
    val request = server.takeRequest()
    request.headers["Authorization"] shouldBe Credentials.basic(basic.username, basic.password)
  }

  @Test
  internal fun `token authentication headers`(server: MockWebServer) {
    server.enqueue(MockResponse())
    server.start()

    val token = ApiTokenAuthentication("mkobit", "0123456789abcdef")
    retrievePluginManagerData(server.url("jenkins"), token)
    val request = server.takeRequest()
    request.headers["Authorization"] shouldBe Credentials.basic(token.username, token.apiToken)
  }
}
