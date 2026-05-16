package testsupport.gradle

import io.kotest.engine.names.WithDataTestName

data class TestedGradleVersion(
  val version: String,
) : WithDataTestName {
  override fun dataTestName() = "Gradle $version"

  companion object {
    val all: List<TestedGradleVersion> =
      System
        .getProperty("test.gradle.versions")
        ?.split(",")
        ?.map { TestedGradleVersion(it.trim()) }
        ?: emptyList()

    // Returns versions from -Ptest.gradle.version=X (or comma-separated X,Y,Z) when set,
    // otherwise all entries. Use with withData(TestedGradleVersion.filtered) to pin during debugging.
    val filtered: List<TestedGradleVersion>
      get() {
        val only = System.getProperty("test.gradle.version") ?: return all
        return only
          .split(",")
          .map { it.trim() }
          .filter { it.isNotBlank() }
          .map { TestedGradleVersion(it) }
      }
  }
}
