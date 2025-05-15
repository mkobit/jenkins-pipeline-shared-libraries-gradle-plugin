package com.mkobit.jenkins.pipelines.http

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasEntry
import strikt.assertions.isEmpty
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
      expectThat(authentication.headers())
        .hasEntry("Authorization", "Basic ${Base64.getEncoder().encodeToString("$USERNAME:$PASSWORD".toByteArray())}")
    }
  }

  @Nested
  inner class ApiTokenAuthenticationTest {
    @Test
    internal fun `headers are present`() {
      val authentication = ApiTokenAuthentication(USERNAME, PASSWORD)
      expectThat(authentication.headers())
        .hasEntry("Authorization", "Basic ${Base64.getEncoder().encodeToString("$USERNAME:$PASSWORD".toByteArray())}")
    }
  }

  @Nested
  inner class AnonymousAuthenticationTest {
    @Test
    internal fun `headers are absent`() {
      expectThat(AnonymousAuthentication.headers()).isEmpty()
    }
  }
}
