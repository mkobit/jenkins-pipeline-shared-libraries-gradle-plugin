/**
 * Describes one functional test target.
 * Null fields mean "not applicable to this variant" — no system property is injected.
 */
data class FunctionalTestVariant(
  /** Full Gradle task name, e.g. "functionalTestJava21". */
  val taskName: String,
  /** Pinned Gradle version → test.gradle.version. Null = no pin. */
  val gradleVersion: String? = null,
  /** Single Jenkins LTS entry → test.jenkins.entries. Null = no Jenkins tests. */
  val jenkinsEntry: JenkinsLtsEntry? = null,
  /** Override test JVM via toolchain. Null = build JVM. */
  val javaVersion: Int? = null,
  /** Default kotest.filter.tags. Null = no default filter. */
  val tagFilter: String? = null,
)
