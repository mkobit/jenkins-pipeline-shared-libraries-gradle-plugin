import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class GenerateJenkinsCompatMatrix : DefaultTask() {
  @get:Nested
  abstract val entries: ListProperty<JenkinsLtsEntry>

  @get:OutputFile
  abstract val outputFile: RegularFileProperty

  @TaskAction
  fun generate() {
    val matrix =
      entries.get().map { (lts, version, bomVersion) ->
        mapOf(
          "jenkins-lts" to lts,
          "jenkins-version" to version,
          "jenkins-bom-version" to bomVersion,
        )
      }
    outputFile.get().asFile.writeText(mapOf("include" to matrix).toJson())
  }
}
