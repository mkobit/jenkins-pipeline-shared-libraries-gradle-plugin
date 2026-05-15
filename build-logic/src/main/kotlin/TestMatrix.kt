import org.gradle.util.GradleVersion

open class TestMatrix {
  val gradleVersions: List<String> =
    (listOf("9.4.1", "9.5.1")
      .map { GradleVersion.version(it) } + GradleVersion.current())
      .toSortedSet()
      .map { it.version }

  val javaVersions: List<Int> = listOf(21, 25)

  val jenkinsLtsEntries: List<JenkinsLtsEntry> =
    listOf(
      JenkinsLtsEntry("2.479.x", "2.479.1", "5054.v620b_5d2b_d5e6"),
      JenkinsLtsEntry("2.528.x", "2.528.3", "6398.v1d26a_dd495e2"),
      JenkinsLtsEntry("2.541.x", "2.541.3", "6364.v16b_76a_4023c7"),
    )
}
