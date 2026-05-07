package testsupport

import com.mkobit.jenkins.pipelines.ci.gradleCompatVersions
import io.kotest.engine.names.WithDataTestName
import org.gradle.util.GradleVersion

data class TestedGradleVersion(
  val version: String,
) : WithDataTestName {
  override fun dataTestName() = "Gradle $version"

  companion object {
    val all: List<TestedGradleVersion> =
      (gradleCompatVersions + GradleVersion.current().version)
        .distinct()
        .map { TestedGradleVersion(it) }

    // Returns only the version matching -Ptest.gradle.version=X when set, otherwise all entries.
    // Use with withData(TestedGradleVersion.filtered) to pin a single version during debugging.
    val filtered: List<TestedGradleVersion>
      get() {
        val only = System.getProperty("test.gradle.version")
        return if (only.isNullOrBlank()) all else all.filter { it.version == only }
      }
  }
}
