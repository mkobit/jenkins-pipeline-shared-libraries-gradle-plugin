package com.mkobit.jenkins.pipelines.codegen

import mu.KotlinLogging
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Files

/**
 * Generates a Groovy class with the specified details.
 */
open class GenerateGroovyFile : DefaultTask() {

  companion object {
    private val LOGGER = KotlinLogging.logger {}
  }

  @get:Input
  var content: String? = null

  @get:Input
  var className: String? = null

  @get:Input
  var packageNamespace: String? = null

  @get:Input
  var imports: List<String> = emptyList()

  @get:OutputDirectory
  val srcDir: DirectoryProperty = project.layout.directoryProperty()

  @get:OutputFile
  val destination: Provider<RegularFile>
    // TODO
    get() = srcDir.map {
      val packageNamespaceDirPath = packageNamespace!!.split(".").joinToString(separator = File.separator)
      it.file("$packageNamespaceDirPath${File.separator}$className.groovy")
    }

  @Suppress("UNUSED")
  @TaskAction
  fun generate() {
    val classFile = destination.get().asFile
    if (classFile.extension != "groovy") {
      throw IllegalStateException("Destination must have groovy extension but file is $classFile")
    }
    LOGGER.info { "Generating class with name $className in package $packageNamespace to file $classFile" }
    val fileContent = """
package $packageNamespace

${imports.joinToString(separator = System.lineSeparator()) { "import $it" }}

$content
""".trimIndent()
    Files.write(classFile.toPath(), fileContent.toByteArray())
  }
}
