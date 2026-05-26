package com.mkobit.jenkins.pipelines

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import kotlin.io.path.outputStream

abstract class ExtractDefaultCodeNarcConfig : DefaultTask() {
  @get:OutputFile
  abstract val outputFile: RegularFileProperty

  @TaskAction
  fun extract() {
    val path = outputFile.get().asFile.toPath()
    val stream =
      SharedLibraryExtension::class.java.classLoader
        .getResourceAsStream("com/mkobit/jenkins/pipelines/codenarc-default.xml")
        ?: error("codenarc-default.xml not found on plugin classpath — packaging bug")
    stream.use { input -> path.outputStream().use { out -> input.copyTo(out) } }
  }
}
