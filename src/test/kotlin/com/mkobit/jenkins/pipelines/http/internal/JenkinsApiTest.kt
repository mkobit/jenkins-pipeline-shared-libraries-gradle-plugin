package com.mkobit.jenkins.pipelines.http.internal

import com.mkobit.jenkins.pipelines.http.AnonymousAuthentication
import com.mkobit.jenkins.pipelines.http.ApiTokenAuthentication
import com.mkobit.jenkins.pipelines.http.BasicAuthentication
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import testsupport.UseMockServer
import testsupport.loadResource
import java.util.stream.Stream

@UseMockServer
internal class JenkinsApiTest constructor(private val server: MockWebServer) {

  companion object {
    @JvmStatic
    private fun authenticationTypes() = Stream.of(
      AnonymousAuthentication,
      BasicAuthentication("mkobit", "password"),
      ApiTokenAuthentication("mkobit", "password")
    )
  }

  @Test
  internal fun `can download GDSL`() {
    server.enqueue(MockResponse().setBody(loadResource("jenkins-data/http/gdsl.txt")))
    server.start()

    downloadGdsl(server.url("jenkins"), AnonymousAuthentication)
    val request = server.takeRequest()
    assertThat(request.path).endsWith("pipeline-syntax/gdsl")
  }
}
