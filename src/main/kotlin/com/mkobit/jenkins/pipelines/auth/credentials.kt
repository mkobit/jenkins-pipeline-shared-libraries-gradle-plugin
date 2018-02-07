package com.mkobit.jenkins.pipelines.auth

interface Credentials {
  val username: String?
  val password: String?
}

data class UsernamePasswordCredentials(override val username: String, override val password: String) : Credentials

object AnonymousCredentials : Credentials {
  override val username: String? = null
  override val password: String? = null
}
