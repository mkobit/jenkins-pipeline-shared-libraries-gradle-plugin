package testsupport

import io.kotest.engine.names.WithDataTestName

data class TestedGradleVersion(
  val version: String,
) : WithDataTestName {
  override fun dataTestName() = "Gradle $version"

  companion object {
    val all: List<TestedGradleVersion> = gradleCompatVersions.map { TestedGradleVersion(it) }

    // Returns only the version matching -Ptest.gradle.version=X when set, otherwise all entries.
    // Use with withData(TestedGradleVersion.filtered) to pin a single version during debugging.
    val filtered: List<TestedGradleVersion>
      get() {
        val only = System.getProperty("test.gradle.version")
        return if (only.isNullOrBlank()) all else all.filter { it.version == only }
      }
  }
}
