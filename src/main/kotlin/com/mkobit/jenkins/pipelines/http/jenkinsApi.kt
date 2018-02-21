package com.mkobit.jenkins.pipelines.http

import okhttp3.Authenticator
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException

private class AuthenticationHeaderAuthenticator(private val authentication: Authentication) : Authenticator {
  override fun authenticate(route: Route?, response: Response?): Request? = authentication.headers().let { headers ->
    if (headers.isEmpty()) {
      null
    } else {
      Request.Builder().headers(Headers.of(headers)).build()
    }
  }
}

@Throws(IOException::class)
fun downloadGdsl(baseUrl: HttpUrl, authentication: Authentication): Response {
  val client = OkHttpClient.Builder()
    .authenticator(AuthenticationHeaderAuthenticator(authentication))
    .build()

  val gdslUrl = baseUrl.newBuilder()
    .addPathSegment("pipeline-syntax")
    .addPathSegment("gdsl")
    .build()

  val request = Request.Builder()
    .url(gdslUrl)
    .get()
    .build()
  return client.newCall(request).execute()
}
