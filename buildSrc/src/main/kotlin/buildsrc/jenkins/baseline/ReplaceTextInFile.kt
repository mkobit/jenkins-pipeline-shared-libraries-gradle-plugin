package buildsrc.jenkins.baseline

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.listProperty
import java.nio.file.Files
import javax.inject.Inject

open class ReplaceTextInFile

@Inject
constructor(objectFactory: ObjectFactory) : DefaultTask() {
  @get:Nested
  val replacements: ListProperty<Replacement> = objectFactory.listProperty<Replacement>().empty()

  @get:InputFile
  val targetFile: RegularFileProperty = objectFactory.fileProperty()

  @TaskAction
  fun replaceText() {
    val path = targetFile.get().asFile.toPath()
    val allReplacements = replacements.get()
    val lines = Files.readAllLines(path, Charsets.UTF_8)
    val newLines =
      lines.map { line ->
        allReplacements.foldRight(
          line,
          { replacement, acc -> replacement.patternAsRegex.replace(acc, replacement.replacement) },
        )
      }
    if (lines == newLines) {
      didWork = false
    } else {
      Files.write(path, newLines, Charsets.UTF_8)
    }
  }
}

data class Replacement(
  @get:Input val pattern: String,
  @get:Internal val replacement: (MatchResult) -> CharSequence,
) {
  constructor(regex: Regex, replacement: (MatchResult) -> CharSequence) : this(regex.pattern, replacement)

  @get:Internal
  val patternAsRegex: Regex = Regex(pattern)
}
