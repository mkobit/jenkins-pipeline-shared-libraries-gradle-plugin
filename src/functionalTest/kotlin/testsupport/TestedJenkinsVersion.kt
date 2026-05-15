package testsupport

import io.kotest.engine.names.WithDataTestName

data class TestedJenkinsVersion(
  val entry: JenkinsCompatEntry,
) : WithDataTestName {
  override fun dataTestName() = "Jenkins ${entry.jenkinsLts}"

  companion object {
    val all: List<TestedJenkinsVersion> = jenkinsCompatEntries.map { TestedJenkinsVersion(it) }

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
