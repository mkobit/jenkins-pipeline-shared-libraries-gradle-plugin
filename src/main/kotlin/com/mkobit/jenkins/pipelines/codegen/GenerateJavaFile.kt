package com.mkobit.jenkins.pipelines.codegen

import com.squareup.javapoet.JavaFile
import mu.KotlinLogging
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

open class GenerateJavaFile @Inject constructor(
  projectLayout: ProjectLayout,
  objectFactory: ObjectFactory
) : DefaultTask() {

  @get:Internal
  val javaFile: Property<JavaFile> = objectFactory.property(JavaFile::class.java)

  // Used to detect when changes to the JavaFile
  @Suppress("UNUSED")
  @get:Input
  val content: Provider<String> = javaFile.map(JavaFile::toString)

  @get:OutputDirectory
  val srcDir: DirectoryProperty = projectLayout.directoryProperty()

  @get:OutputFile
  val destination: Provider<RegularFile> = srcDir.let { dir ->
    dir.file(javaFile.map {
      it.packageName.split(".") + listOf("${it.typeSpec.name}.java")
    }.map { it.joinToString(separator = File.separator)})
  }

  companion object {
    private val LOGGER = KotlinLogging.logger {}
  }

  @TaskAction
  fun createFile() {
    val java = javaFile.get()
    val destinationPath = destination.get().asFile.toPath()
    LOGGER.info { "Generating Java file at $destinationPath for class ${java.typeSpec.name} in package ${java.packageName}" }
    java.writeTo(destinationPath)
  }
}
