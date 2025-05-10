package buildsrc.jenkins.baseline

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getValue
import java.io.File
import java.time.Duration

class JenkinsRebaselineToolsPlugin : Plugin<Project> {
  private val xmlMapper: XmlMapper by lazy { XmlMapper() }

  private val objectMapper: ObjectMapper by lazy { ObjectMapper() }

  companion object {
    private val defaultUpToDateDownloadDuration = Duration.ofDays(1L)
    private const val TASK_GROUP = "Development"
  }

  override fun apply(target: Project) {
    target.run {
      val stableDownloadDirectory = layout.buildDirectory.dir(splitBySeparator("updateCenter", "stable"))

      val downloadLatestCoreVersion = tasks.register("downloadLatestCoreVersion", DownloadFile::class.java) {
        baseUrl.set("https://updates.jenkins.io/")
        downloadPath.set("stable/latestCore.txt")
        destination.set(stableDownloadDirectory.map { it.file("latestCore.txt") })
        upToDateDuration.set(defaultUpToDateDownloadDuration)
      }

      val downloadUpdateCenterJson = tasks.register("downloadUpdateCenterJson", DownloadFile::class.java) {
        baseUrl.set("https://updates.jenkins.io/")
        downloadPath.set("stable/update-center.actual.json")
        destination.set(stableDownloadDirectory.map { it.file("update-center.actual.json") })
        upToDateDuration.set(defaultUpToDateDownloadDuration)
      }
      val downloadJenkinsTestHarnessManifest =
        tasks.register("downloadJenkinsTestHarnessManifest", DownloadFile::class.java) {
          baseUrl.set("https://repo.jenkins-ci.org/public/")
          downloadPath.set("org/jenkins-ci/main/jenkins-test-harness/maven-metadata.xml ")
          destination.set(stableDownloadDirectory.map { it.file("jenkins-test-harness-maven-metadata.xml") })
          upToDateDuration.set(defaultUpToDateDownloadDuration)
        }

      val updateCenter: Provider<JsonNode> =
        downloadUpdateCenterJson.get().destination.map { objectMapper.readTree(it.asFile) }
      val ltsVersion: Provider<String> = downloadLatestCoreVersion.get().destination.map { it.asFile.readText().trim() }
      val testHarnessVersion: Provider<String> =
        downloadJenkinsTestHarnessManifest.get().destination
          .map { xmlMapper.readTree(it.asFile) }
          .map { latestVersionFromManifest(it) }
      val workflowApiVersion: Provider<String> = updateCenter.map { versionForPlugin(it, "workflow-api") }
      val workflowBasicStepsVersion: Provider<String> =
        updateCenter.map { versionForPlugin(it, "workflow-basic-steps") }
      val workflowCpsVersion: Provider<String> = updateCenter.map { versionForPlugin(it, "workflow-cps") }
      val workflowDurableTaskStepVersion: Provider<String> =
        updateCenter.map { versionForPlugin(it, "workflow-durable-task-step") }
      val workflowCpsGlobalLibVersion: Provider<String> =
        updateCenter.map { versionForPlugin(it, "workflow-cps-global-lib") }
      val workflowMultibranchVersion: Provider<String> =
        updateCenter.map { versionForPlugin(it, "workflow-multibranch") }
      val workflowJobVersion: Provider<String> = updateCenter.map { versionForPlugin(it, "workflow-job") }
      val workflowScmStepVersion: Provider<String> = updateCenter.map { versionForPlugin(it, "workflow-scm-step") }
      val workflowStepApiVersion: Provider<String> = updateCenter.map { versionForPlugin(it, "workflow-step-api") }
      val workflowSupportVersion: Provider<String> = updateCenter.map { versionForPlugin(it, "workflow-support") }

      val updateSharedLibraryPluginVersions =
        tasks.register("updateSharedLibraryPluginVersions", ReplaceTextInFile::class.java) {
          group = TASK_GROUP
          description =
            "Updates the values in SharedLibraryPlugin.kt to the latest LTS and versions from the update center"
          dependsOn(downloadLatestCoreVersion, downloadUpdateCenterJson, downloadJenkinsTestHarnessManifest)
          targetFile.set(
            layout.projectDirectory.file(
              splitBySeparator(
                "src",
                "main",
                "kotlin",
                "com",
                "mkobit",
                "jenkins",
                "pipelines",
                "SharedLibraryPlugin.kt",
              ),
            ),
          )
          replacements.run {
            add(ltsVersion.map { pluginConstantReplacement("DEFAULT_CORE_VERSION", it) })
            add(testHarnessVersion.map { pluginConstantReplacement("DEFAULT_TEST_HARNESS_VERSION", it) })
            add(workflowApiVersion.map { pluginConstantReplacement("DEFAULT_WORKFLOW_API_PLUGIN_VERSION", it) })
            add(
              workflowBasicStepsVersion.map {
                pluginConstantReplacement(
                  "DEFAULT_WORKFLOW_BASIC_STEPS_PLUGIN_VERSION",
                  it,
                )
              },
            )
            add(workflowCpsVersion.map { pluginConstantReplacement("DEFAULT_WORKFLOW_CPS_PLUGIN_VERSION", it) })
            add(
              workflowDurableTaskStepVersion.map {
                pluginConstantReplacement(
                  "DEFAULT_WORKFLOW_DURABLE_TASK_STEP_PLUGIN_VERSION",
                  it,
                )
              },
            )
            add(
              workflowCpsGlobalLibVersion.map {
                pluginConstantReplacement(
                  "DEFAULT_WORKFLOW_GLOBAL_CPS_LIBRARY_PLUGIN_VERSION",
                  it,
                )
              },
            )
            add(workflowJobVersion.map { pluginConstantReplacement("DEFAULT_WORKFLOW_JOB_PLUGIN_VERSION", it) })
            add(
              workflowMultibranchVersion.map {
                pluginConstantReplacement(
                  "DEFAULT_WORKFLOW_MULTIBRANCH_PLUGIN_VERSION",
                  it,
                )
              },
            )
            add(workflowStepApiVersion.map {
              pluginConstantReplacement(
                "DEFAULT_WORKFLOW_STEP_API_PLUGIN_VERSION",
                it
              )
            })
            add(workflowScmStepVersion.map {
              pluginConstantReplacement(
                "DEFAULT_WORKFLOW_SCM_STEP_PLUGIN_VERSION",
                it
              )
            })
            add(workflowSupportVersion.map { pluginConstantReplacement("DEFAULT_WORKFLOW_SUPPORT_PLUGIN_VERSION", it) })
          }
        }

      val updateBuildGradleVersionsForDependencyTools = tasks.register("updateBuildGradleVersionsForDependencyTools", ReplaceTextInFile::class.java) {
        dependsOn(downloadLatestCoreVersion, downloadUpdateCenterJson, downloadJenkinsTestHarnessManifest)
        group = TASK_GROUP
        description = "Updates the values in build.gradle.kts to the latest LTS and versions from the update center"
        targetFile.set(layout.projectDirectory.file("build.gradle.kts"))
        replacements.run {
          add(ltsVersion.map { moduleDefinitionReplacement("org.jenkins-ci.main:jenkins-core", it) })
          add(testHarnessVersion.map { moduleDefinitionReplacement("org.jenkins-ci.main:jenkins-test-harness", it) })
          add(
            workflowApiVersion.map {
              moduleDefinitionReplacement(
                "org.jenkins-ci.plugins.workflow:workflow-api",
                it,
              )
            },
          )
          add(
            workflowBasicStepsVersion.map {
              moduleDefinitionReplacement(
                "org.jenkins-ci.plugins.workflow:workflow-basic-steps",
                it,
              )
            },
          )
          add(
            workflowCpsVersion.map {
              moduleDefinitionReplacement(
                "org.jenkins-ci.plugins.workflow:workflow-cps",
                it,
              )
            },
          )
          add(
            workflowDurableTaskStepVersion.map {
              moduleDefinitionReplacement(
                "org.jenkins-ci.plugins.workflow:workflow-durable-task-step",
                it,
              )
            },
          )
          add(
            workflowCpsGlobalLibVersion.map {
              moduleDefinitionReplacement(
                "org.jenkins-ci.plugins.workflow:workflow-cps-global-lib",
                it,
              )
            },
          )
          add(
            workflowJobVersion.map {
              moduleDefinitionReplacement(
                "org.jenkins-ci.plugins.workflow:workflow-job",
                it,
              )
            },
          )
          add(
            workflowMultibranchVersion.map {
              moduleDefinitionReplacement(
                "org.jenkins-ci.plugins.workflow:workflow-multibranch",
                it,
              )
            },
          )
          add(
            workflowStepApiVersion.map {
              moduleDefinitionReplacement(
                "org.jenkins-ci.plugins.workflow:workflow-step-api",
                it,
              )
            },
          )
          add(
            workflowScmStepVersion.map {
              moduleDefinitionReplacement(
                "org.jenkins-ci.plugins.workflow:workflow-scm-step",
                it,
              )
            },
          )
          add(
            workflowSupportVersion.map {
              moduleDefinitionReplacement(
                "org.jenkins-ci.plugins.workflow:workflow-support",
                it,
              )
            },
          )
        }
      }

      @Suppress("UNUSED_VARIABLE")
      val runRebaseline by tasks.register("runRebaseline") {
        group = TASK_GROUP
        description = "Executes tasks to rebaseline project"
        dependsOn(updateSharedLibraryPluginVersions, updateBuildGradleVersionsForDependencyTools)
      }
    }
  }

  /**
   * Retrieves the plugin version for the given [pluginId] from the [jsonNode].
   * @param jsonNode the update center JSON response
   */
  private fun versionForPlugin(
    jsonNode: JsonNode,
    pluginId: String,
  ): String =
    jsonNode
      .get("plugins")
      .get(pluginId)
      .get("version")
      .textValue()

  /**
   * Retrieves the latest version from the [jsonNode].
   * @param jsonNode the `maven-metadata.xml` file
   */
  private fun latestVersionFromManifest(jsonNode: JsonNode): String =
    jsonNode.get("versioning").get("release").textValue()

  /**
   * Creates a [Replacement] to update the constant value for the given [name] and [newVersion].
   * This replacement is for use with how constants are currently defined in `SharedLibraryPlugin.kt`.
   */
  private fun pluginConstantReplacement(
    name: String,
    newVersion: String,
  ): Replacement =
    Replacement(
      Regex("""(^\s+private const val $name = ")[\d\\.]+(")${'$'}"""),
    ) { result -> "${result.groupValues[1]}$newVersion${result.groupValues[2]}" }

  /**
   * Creates a [Replacement] to update the constant value for the given [groupArtifact] and [newVersion].
   * This replacement is for use with the constant definition currently in the `build.gradle.kts`.
   */
  private fun moduleDefinitionReplacement(
    groupArtifact: String,
    newVersion: String,
  ): Replacement =
    Replacement(
      Regex("""($groupArtifact:)[\d\\.]+"""),
    ) { result -> "${result.groupValues[1]}$newVersion" }

  private fun splitBySeparator(
    first: String,
    vararg rest: String,
  ): String = rest.fold(first, { accumulator, string -> "$accumulator${File.separator}$string" })
}
