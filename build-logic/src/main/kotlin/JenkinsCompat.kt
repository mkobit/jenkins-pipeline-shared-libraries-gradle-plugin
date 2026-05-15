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

  operator fun component1(): Property<String> = lts
  operator fun component2(): Property<String> = version
  operator fun component3(): Property<String> = bomVersion
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
      entries.get().map { (lts, version, bomVersion) ->
        mapOf(
          "jenkins-lts" to lts.get(),
          "jenkins-version" to version.get(),
          "jenkins-bom-version" to bomVersion.get(),
        )
      }
    outputFile.get().asFile.writeText(mapOf("include" to matrix).toJson())
  }
}
