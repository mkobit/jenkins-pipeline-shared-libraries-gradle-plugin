package testsupport

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
        ?: run {
          System.err.println(
            "[WARNING] test.gradle.versions system property is not set — " +
              "TestedGradleVersion.all is empty. " +
              "Ensure the ci-tasks convention plugin is applied to the test task.",
          )
          emptyList()
        }

    // Returns versions matching -Ptest.gradle.version=X (or comma-separated X,Y,Z) when set,
    // otherwise all entries. Use with withData(TestedGradleVersion.filtered) to pin during debugging.
    val filtered: List<TestedGradleVersion>
      get() {
        val only = System.getProperty("test.gradle.version") ?: return all
        val targets =
          only
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
        return all.filter { it.version in targets }
      }
  }
}
