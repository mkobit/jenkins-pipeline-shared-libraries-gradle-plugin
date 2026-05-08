package testsupport

import com.mkobit.jenkins.pipelines.ci.JenkinsCompatEntry
import com.mkobit.jenkins.pipelines.ci.jenkinsCompatEntries
import io.kotest.engine.names.WithDataTestName

data class TestedJenkinsVersion(
  val entry: JenkinsCompatEntry,
) : WithDataTestName {
  override fun dataTestName() = "Jenkins ${entry.jenkinsLts}"

  companion object {
    val all: List<TestedJenkinsVersion> =
      jenkinsCompatEntries.map { TestedJenkinsVersion(it) }

    // Returns only the entry matching -Ptest.jenkins.version=X when set, otherwise all entries.
    val filtered: List<TestedJenkinsVersion>
      get() {
        val only = System.getProperty("test.jenkins.version")
        return if (only.isNullOrBlank()) all else all.filter { it.entry.jenkinsVersion == only }
      }
  }
}
