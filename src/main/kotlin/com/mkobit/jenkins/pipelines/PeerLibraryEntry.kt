package com.mkobit.jenkins.pipelines

import java.io.Serializable

/**
 * Resolved peer-library entry passed to [PeerLibrariesArgumentProvider] at integration-test time.
 *
 * Built at execution time by zipping the resolved `sharedLibrarySourceElements` artefacts with
 * the consumer's declared [PeerLibrarySpec]s. Each entry contributes a contiguous triple of
 * `test.library.N.{name,location,implicit}` system properties to every Jenkins-wired test suite.
 *
 * Plain `Serializable` value type rather than a `Property`-backed abstract class — this lets
 * the build script construct entries inside a `Provider` transform without capturing
 * `ObjectFactory`, keeping the chain configuration-cache-friendly.
 */
data class PeerLibraryEntry(
  val libraryName: String,
  val locationPath: String,
  val implicit: Boolean,
) : Serializable {
  companion object {
    private const val serialVersionUID: Long = 1L
  }
}
