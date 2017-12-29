package buildsrc.jenkins.baseline

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.getValue
import java.io.File
import java.time.Duration

open class JenkinsRebaselineToolsPlugin : Plugin<Project> {
  companion object {
    private val defaultUpToDateDownloadDuration = Duration.ofDays(1L)
  }
  override fun apply(target: Project) {
    target.run {
      val stableDownloadDirectory = layout.buildDirectory.dir("updateCenter${File.separator}stable${File.separator}")
      val downloadLatestCoreVersion by tasks.creating(DownloadFile::class) {
        baseUrl.set("https://updates.jenkins.io/")
        downloadPath.set("stable/update-center.actual.json")
        destination.set(stableDownloadDirectory.map { it.file("latestCore.txt") })
        upToDateDuration.set(defaultUpToDateDownloadDuration)
      }
      val downloadUpdateCenterJson by tasks.creating(DownloadFile::class) {
        baseUrl.set("https://updates.jenkins.io/")
        downloadPath.set("stable/update-center.actual.json")
        destination.set(stableDownloadDirectory.map { it.file("update-center.actual.json") })
        upToDateDuration.set(defaultUpToDateDownloadDuration)
      }
    }
  }
}
