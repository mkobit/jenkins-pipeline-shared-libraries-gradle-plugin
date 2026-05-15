package com.mkobit.jenkins.pipelines

import kotlin.io.path.outputStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class ExtractJenkinsCodeNarcConfig : DefaultTask() {
  @get:OutputFile
  abstract val outputFile: RegularFileProperty

  @TaskAction
  fun extract() {
    val path = outputFile.get().asFile.toPath()
    SharedLibraryExtension::class.java.classLoader
      .getResourceAsStream("com/mkobit/jenkins/pipelines/codenarc-jenkins.xml")!!
      .use { input -> path.outputStream().use { out -> input.copyTo(out) } }
  }
}
