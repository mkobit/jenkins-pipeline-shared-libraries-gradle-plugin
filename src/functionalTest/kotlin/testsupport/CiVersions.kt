package testsupport

import org.gradle.util.GradleVersion

data class JenkinsCompatEntry(
  val jenkinsLts: String,
  val jenkinsVersion: String,
  val jenkinsBomVersion: String,
)

val gradleCompatVersions: List<String> =
  (
    listOf("9.0.0", "9.1.0", "9.2.1", "9.3.1", "9.4.1", "9.5.0")
      .map { GradleVersion.version(it) } + GradleVersion.current()
  ).toSortedSet()
    .map { it.version }

val jenkinsCompatEntries: List<JenkinsCompatEntry> =
  listOf(
    JenkinsCompatEntry("2.479.x", "2.479.1", "5054.v620b_5d2b_d5e6"),
    JenkinsCompatEntry("2.528.x", "2.528.3", "6398.v1d26a_dd495e2"),
    JenkinsCompatEntry("2.541.x", "2.541.3", "6364.v16b_76a_4023c7"),
  )
