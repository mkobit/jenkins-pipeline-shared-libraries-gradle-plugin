package com.mkobit.jenkins.pipelines.http

import okhttp3.Credentials

/**
 * Provides authentication for an HTTP request.
 */
interface Authentication {
  fun headers(): Map<String, String>
}

/**
 * Basic authentication authentication.
 * It is generally preferred to use an API key [ApiTokenAuthentication].
 */
data class BasicAuthentication(private val username: String, private val password: String) : Authentication {
  override fun headers(): Map<String, String> = mapOf("Authentication" to Credentials.basic(username, password))
}

/**
 * Authentication using API token.
 */
data class ApiTokenAuthentication(private val username: String, private val apiToken: String) : Authentication {
  override fun headers(): Map<String, String> = mapOf("Authentication" to Credentials.basic(username, apiToken))
}

/**
 * Anonymous user authentication.
 */
object AnonymousAuthentication : Authentication {
  override fun headers(): Map<String, String> = emptyMap()
}
