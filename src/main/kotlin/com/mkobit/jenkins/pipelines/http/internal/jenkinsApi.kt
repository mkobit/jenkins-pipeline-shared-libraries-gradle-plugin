package com.mkobit.jenkins.pipelines.http.internal

import com.mkobit.jenkins.pipelines.http.Authentication
import okhttp3.Authenticator
import okhttp3.ConnectionPool
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Simple [Authenticator] that creates a [Request] with the authentication headers provided by the [authentication].
 */
private class BasicAuthHeaderInterceptor(
  private val authentication: Authentication
) : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response =
    authentication.headers().let { headers ->
      val request =
        if (headers.isNotEmpty()) {
          chain
            .request()
            .newBuilder()
            .headers(Headers.of(headers))
            .build()
        } else {
          chain.request()
        }
      chain.proceed(request)
    }
}

private fun newSinglePoolClient(
  baseUrl: HttpUrl,
  authentication: Authentication
): OkHttpClient =
  OkHttpClient
    .Builder()
    .addInterceptor(BasicAuthHeaderInterceptor(authentication))
    .connectionPool(ConnectionPool(1, 1L, TimeUnit.MINUTES))
    .build()

/**
 * Downloads the GDSL from the Jenkins instance located at [baseUrl].
 * @param baseUrl the base URL of the Jenkins instance
 */
@Throws(IOException::class)
internal fun downloadGdsl(
  baseUrl: HttpUrl,
  authentication: Authentication
): Response {
  val client = newSinglePoolClient(baseUrl, authentication)

  val gdslUrl =
    baseUrl
      .newBuilder()
      .addPathSegment("pipeline-syntax")
      .addPathSegment("gdsl")
      .build()

  val request =
    Request
      .Builder()
      .url(gdslUrl)
      .get()
      .build()
  return client.newCall(request).execute()
}

/**
 * Retrieves the JSON plugin manager data from the Jenkins instance located at [baseUrl].
 * @param baseUrl the base URL of the Jenkins instance
 */
@Throws(IOException::class)
internal fun retrievePluginManagerData(
  baseUrl: HttpUrl,
  authentication: Authentication
): Response {
  val client = newSinglePoolClient(baseUrl, authentication)

  val pluginManagerUrl =
    baseUrl
      .newBuilder()
      .addPathSegment("pluginManager")
      .addPathSegment("api")
      .addPathSegment("json")
      .addQueryParameter("depth", 2.toString())
      .addQueryParameter("pretty", true.toString())
      .build()

  val request =
    Request
      .Builder()
      .url(pluginManagerUrl)
      .get()
      .build()

  return client.newCall(request).execute()
}

@Throws(IOException::class)
internal fun connect(
  baseUrl: HttpUrl,
  authentication: Authentication
): Response {
  val client = newSinglePoolClient(baseUrl, authentication)

  val gdslUrl =
    baseUrl
      .newBuilder()
      .build()

  val request =
    Request
      .Builder()
      .url(gdslUrl)
      .head()
      .build()
  return client.newCall(request).execute()
}
