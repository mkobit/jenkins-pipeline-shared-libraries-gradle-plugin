import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class GenerateBuildConfig : DefaultTask() {
  @get:Input
  abstract val gradleVersion: Property<String>

  @get:Input
  abstract val javaVersion: Property<Int>

  @get:OutputFile
  abstract val outputFile: RegularFileProperty

  @TaskAction
  fun generate() {
    outputFile.get().asFile.writeText("""{"gradle-version":"${gradleVersion.get()}","java-version":${javaVersion.get()}}""")
  }
}
