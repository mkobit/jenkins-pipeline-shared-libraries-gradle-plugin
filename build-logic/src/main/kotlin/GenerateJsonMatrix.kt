import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class GenerateJsonMatrix : DefaultTask() {
  @get:Input
  abstract val matrixEntries: ListProperty<Map<String, String>>

  @get:OutputFile
  abstract val outputFile: RegularFileProperty

  @TaskAction
  fun generate() {
    outputFile.get().asFile.writeText(mapOf("include" to matrixEntries.get()).toJson())
  }
}
