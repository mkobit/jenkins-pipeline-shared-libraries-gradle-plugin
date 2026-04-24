package com.mkobit.jenkins.pipelines.http.internal

import com.mkobit.jenkins.pipelines.http.AnonymousAuthentication
import com.mkobit.jenkins.pipelines.http.ApiTokenAuthentication
import com.mkobit.jenkins.pipelines.http.BasicAuthentication
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import okhttp3.Credentials
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

internal class JenkinsApiTest : DescribeSpec({
  lateinit var server: MockWebServer

  beforeTest {
    server = MockWebServer()
    server.enqueue(MockResponse())
    server.start()
  }

  afterTest {
    server.shutdown()
  }

  it("retrieve GDSL sends GET to pipeline-syntax/gdsl") {
    downloadGdsl(server.url("jenkins"), AnonymousAuthentication)
    val request = server.takeRequest()
    request.method shouldBe "GET"
    request.path shouldEndWith "pipeline-syntax/gdsl"
  }

  it("plugin manager request targets the correct path with depth parameter") {
    retrievePluginManagerData(server.url("jenkins"), AnonymousAuthentication)
    val request = server.takeRequest()
    request.method shouldBe "GET"
    request.requestUrl!!.pathSegments shouldBe listOf("jenkins", "pluginManager", "api", "json")
    request.requestUrl!!.queryParameter("depth") shouldBe "2"
  }

  it("connect sends HEAD to the base URL") {
    connect(server.url("jenkins"), AnonymousAuthentication)
    val request = server.takeRequest()
    request.method shouldBe "HEAD"
    request.requestUrl!!.pathSegments shouldBe listOf("jenkins")
  }

  describe("authentication headers") {
    it("BasicAuthentication sends a Basic Authorization header") {
      val basic = BasicAuthentication("mkobit", "hunter2")
      retrievePluginManagerData(server.url("jenkins"), basic)
      server.takeRequest().headers["Authorization"] shouldBe Credentials.basic(basic.username, basic.password)
    }

    it("ApiTokenAuthentication sends a Basic Authorization header") {
      val token = ApiTokenAuthentication("mkobit", "0123456789abcdef")
      retrievePluginManagerData(server.url("jenkins"), token)
      server.takeRequest().headers["Authorization"] shouldBe Credentials.basic(token.username, token.apiToken)
    }
  }
})
