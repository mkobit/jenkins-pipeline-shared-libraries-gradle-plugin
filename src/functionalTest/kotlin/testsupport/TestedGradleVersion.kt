package testsupport

import io.kotest.engine.names.WithDataTestName

enum class TestedGradleVersion(
  val version: String,
) : WithDataTestName {
  V9_0("9.0"),
  V9_4_1("9.4.1"),
  ;

  override fun dataTestName() = "Gradle $version"
}
