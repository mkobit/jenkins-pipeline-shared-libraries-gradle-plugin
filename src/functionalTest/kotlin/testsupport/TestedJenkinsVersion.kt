package testsupport

import io.kotest.engine.names.WithDataTestName

data class JenkinsCompatEntry(
  val jenkinsLts: String,
  val jenkinsVersion: String,
  val jenkinsBomVersion: String,
)

data class TestedJenkinsVersion(
  val entry: JenkinsCompatEntry,
) : WithDataTestName {
  override fun dataTestName() = "Jenkins ${entry.jenkinsLts}"

  companion object {
    val all: List<TestedJenkinsVersion> =
      System
        .getProperty("test.jenkins.entries")
        ?.split(",")
        ?.map { raw ->
          val (lts, version, bom) = raw.split("|")
          TestedJenkinsVersion(JenkinsCompatEntry(lts, version, bom))
        }
        ?: emptyList()

    // Returns entries matching -Ptest.jenkins.version=X (or comma-separated X,Y,Z) when set,
    // otherwise all entries.
    val filtered: List<TestedJenkinsVersion>
      get() {
        val only = System.getProperty("test.jenkins.version") ?: return all
        val targets =
          only
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
        return all.filter { it.entry.jenkinsVersion in targets }
      }
  }
}
