package com.mkobit.jenkins.pipelines.http

import okhttp3.Credentials

/**
 * Authentication for an HTTP request.
 * This only supports preemptive authentication
 * @see BasicAuthentication
 * @see ApiTokenAuthentication
 * @see AnonymousAuthentication
 */
interface Authentication {
  /**
   * Provides the headers for preemptive authentication.
   */
  fun headers(): Map<String, String>
}

/**
 * Basic authentication authentication.
 * It is generally preferred to use an API key [ApiTokenAuthentication].
 * @property username the username to authenticate with
 * @property password the password for the [username]
 */
data class BasicAuthentication(val username: String, val password: String) : Authentication {
  override fun headers(): Map<String, String> = mapOf("Authentication" to Credentials.basic(username, password))
}

/**
 * Authentication using API token.
 * @property username the username to authenticate with
 * @property apiToken the API token for the [username]
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
