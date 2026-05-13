import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

interface JenkinsMatrixEntry {
  @get:Input val lts: Property<String>

  @get:Input val version: Property<String>

  @get:Input val bomVersion: Property<String>
}

@CacheableTask
abstract class GenerateJenkinsCompatMatrix : DefaultTask() {
  @get:Nested
  abstract val entries: ListProperty<JenkinsMatrixEntry>

  @get:OutputFile
  abstract val outputFile: RegularFileProperty

  @TaskAction
  fun generate() {
    val matrix =
      entries.get().map { e ->
        mapOf(
          "jenkins-lts" to e.lts.get(),
          "jenkins-version" to e.version.get(),
          "jenkins-bom-version" to e.bomVersion.get(),
        )
      }
    outputFile.get().asFile.writeText(mapOf("include" to matrix).toJson())
  }
}
