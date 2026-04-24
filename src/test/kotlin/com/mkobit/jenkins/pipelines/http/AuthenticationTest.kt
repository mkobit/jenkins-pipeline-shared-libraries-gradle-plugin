package com.mkobit.jenkins.pipelines.http

import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContain
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Base64

internal class AuthenticationTest {
  private companion object {
    private const val USERNAME = "mkobit"
    private const val PASSWORD = "this is the password"
  }

  @Nested
  inner class BasicAuthenticationTest {
    @Test
    internal fun `headers are present`() {
      val authentication = BasicAuthentication(USERNAME, PASSWORD)
      authentication.headers().shouldContain(
        "Authorization",
        "Basic ${Base64.getEncoder().encodeToString("$USERNAME:$PASSWORD".toByteArray())}",
      )
    }
  }

  @Nested
  inner class ApiTokenAuthenticationTest {
    @Test
    internal fun `headers are present`() {
      val authentication = ApiTokenAuthentication(USERNAME, PASSWORD)
      authentication.headers().shouldContain(
        "Authorization",
        "Basic ${Base64.getEncoder().encodeToString("$USERNAME:$PASSWORD".toByteArray())}",
      )
    }
  }

  @Nested
  inner class AnonymousAuthenticationTest {
    @Test
    internal fun `headers are absent`() {
      AnonymousAuthentication.headers().shouldBeEmpty()
    }
  }
}
