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
data class BasicAuthentication(val username: String, val password: String) : Authentication {
  override fun headers(): Map<String, String> = mapOf("Authentication" to Credentials.basic(username, password))
}

/**
 * Authentication using API token.
 */
data class ApiTokenAuthentication(val username: String, val apiToken: String) : Authentication {
  override fun headers(): Map<String, String> = mapOf("Authentication" to Credentials.basic(username, apiToken))
}

/**
 * Anonymous user authentication.
 */
object AnonymousAuthentication : Authentication {
  override fun headers(): Map<String, String> = emptyMap()
}
