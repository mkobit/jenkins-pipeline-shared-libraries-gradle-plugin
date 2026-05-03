package testsupport

import io.kotest.engine.names.WithDataTestName

enum class TestedGradleVersion(
  val version: String,
) : WithDataTestName {
  V9_0_0("9.0.0"),
  V9_1_0("9.1.0"),
  V9_2_1("9.2.1"),
  V9_3_1("9.3.1"),
  V9_4_1("9.4.1"),
  V9_5_0("9.5.0"),
  ;

  override fun dataTestName() = "Gradle $version"
}
