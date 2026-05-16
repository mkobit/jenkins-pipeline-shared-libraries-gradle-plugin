import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class GenerateJsonMatrix : DefaultTask() {
  @get:Nested
  abstract val matrixEntries: ListProperty<MatrixEntry>

  @get:OutputFile
  abstract val outputFile: RegularFileProperty

  @TaskAction
  fun generate() {
    outputFile.get().asFile.writeText(mapOf("include" to matrixEntries.get().map { it.fields }).toJson())
  }
}
