package com.mkobit.jenkins.pipelines

import java.io.Serializable

// Serializable value type so Provider transforms stay config-cache-friendly without capturing ObjectFactory.
data class SharedLibraryEntry(
  val libraryName: String,
  val locationPath: String,
  val implicit: Boolean,
) : Serializable {
  companion object {
    private const val serialVersionUID: Long = 1L
  }
}
