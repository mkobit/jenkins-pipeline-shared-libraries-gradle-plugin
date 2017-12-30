package buildsrc.jenkins.baseline

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.getValue
import java.io.File
import java.time.Duration

open class JenkinsRebaselineToolsPlugin : Plugin<Project> {
  companion object {
    private val defaultUpToDateDownloadDuration = Duration.ofDays(1L)
    private val gavRegex = Regex("""^.*:.*:([\d\\.]*)${'$'}""")
  }
  override fun apply(target: Project) {
    target.run {
      val stableDownloadDirectory = layout.buildDirectory.dir(splitBySeparator("updateCenter", "stable"))
      val downloadLatestCoreVersion by tasks.creating(DownloadFile::class) {
        baseUrl.set("https://updates.jenkins.io/")
        downloadPath.set("stable/latestCore.txt")
        destination.set(stableDownloadDirectory.map { it.file("latestCore.txt") })
        upToDateDuration.set(defaultUpToDateDownloadDuration)
      }
      val downloadUpdateCenterJson by tasks.creating(DownloadFile::class) {
        baseUrl.set("https://updates.jenkins.io/")
        downloadPath.set("stable/update-center.actual.json")
        destination.set(stableDownloadDirectory.map { it.file("update-center.actual.json") })
        upToDateDuration.set(defaultUpToDateDownloadDuration)
      }

      val updateCenter: Provider<JsonNode> = downloadUpdateCenterJson.destination.map { ObjectMapper().readTree(it.asFile) }
      val ltsVersion: Provider<String> = downloadLatestCoreVersion.destination.map { it.asFile.readText().trim() }

      val updateSharedLibraryPlugin by tasks.creating(ReplaceTextInFile::class) {
        group = "Development"
        description = "Updates the values in SharedLibraryPlugin.kt to the latest LTS and versions from the update center"
        dependsOn(downloadLatestCoreVersion, downloadUpdateCenterJson)
        targetFile.set(layout.projectDirectory.file(splitBySeparator("src", "main", "kotlin", "com", "mkobit", "jenkins", "pipelines", "SharedLibraryPlugin.kt")))
        replacements.run {
          // groovy version
//          add(ltsVersion.map { constantReplacement("DEFAULT_GROOVY_VERSION", it) })
          add(ltsVersion.map { constantReplacement("DEFAULT_CORE_VERSION", it) })
//          add(ltsVersion.map { constantReplacement("DEFAULT_TEST_HARNESS_VERSION", it) })
          add(updateCenter.map { constantReplacement("DEFAULT_WORKFLOW_API_PLUGIN_VERSION", versionForPlugin(it, "workflow-api")) })
          add(updateCenter.map { constantReplacement("DEFAULT_WORKFLOW_BASIC_STEPS_PLUGIN_VERSION", versionForPlugin(it, "workflow-basic-steps")) })
          add(updateCenter.map { constantReplacement("DEFAULT_WORKFLOW_CPS_PLUGIN_VERSION", versionForPlugin(it, "workflow-cps")) })
          add(updateCenter.map { constantReplacement("DEFAULT_WORKFLOW_DURABLE_TASK_STEP_PLUGIN_VERSION", versionForPlugin(it, "workflow-durable-task-step")) })
          add(updateCenter.map { constantReplacement("DEFAULT_WORKFLOW_GLOBAL_CPS_LIBRARY_PLUGIN_VERSION", versionForPlugin(it, "workflow-cps-global-lib")) })
          add(updateCenter.map { constantReplacement("DEFAULT_WORKFLOW_JOB_PLUGIN_VERSION", versionForPlugin(it, "workflow-job")) })
          add(updateCenter.map { constantReplacement("DEFAULT_WORKFLOW_MULTIBRANCH_PLUGIN_VERSION", versionForPlugin(it, "workflow-multibranch")) })
          add(updateCenter.map { constantReplacement("DEFAULT_WORKFLOW_STEP_API_PLUGIN_VERSION", versionForPlugin(it, "workflow-scm-step")) })
          add(updateCenter.map { constantReplacement("DEFAULT_WORKFLOW_SCM_STEP_PLUGIN_VERSION", versionForPlugin(it, "workflow-step-api")) })
          add(updateCenter.map { constantReplacement("DEFAULT_WORKFLOW_SUPPORT_PLUGIN_VERSION", versionForPlugin(it, "workflow-support")) })
        }
      }
    }
  }

  private fun versionForPlugin(jsonNode: JsonNode, pluginId: String): String {
    return jsonNode.get("plugins").get(pluginId).get("version").textValue()
  }


  private fun constantReplacement(name: String, newVersion: String): Replacement {
    return Replacement(
      Regex("""(^\s+private val $name = ")[\d\\.]+(")${'$'}"""),
      { result -> "${result.groupValues[1]}$newVersion${result.groupValues[2]}"}
    )
  }

  private fun splitBySeparator(first: String, vararg rest: String): String =
    rest.toList().fold(first, { accumulator, string -> "$accumulator${File.separator}$string" })
}
