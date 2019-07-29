package com.mkobit.jenkins.pipelines.codegen

import com.squareup.javapoet.JavaFile
import mu.KotlinLogging
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
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

/**
 * Generates a Java file into the [srcDir].
 */
open class GenerateJavaFile @Inject constructor(
  objectFactory: ObjectFactory
) : DefaultTask() {

  /**
   * [JavaFile] to write.
   */
  @get:Internal
  val javaFile: Property<JavaFile> = objectFactory.property(JavaFile::class.java)

  /**
   * The content of the [javaFile].
   */
  // Used to detect when changes to the JavaFile
  @Suppress("UNUSED")
  @get:Input
  val content: Provider<String> = javaFile.map(JavaFile::toString)

  /**
   * The root directory to create the source file in.
   */
  @get:OutputDirectory
  val srcDir: DirectoryProperty = objectFactory.directoryProperty()

  /**
   * The destination of the Java file.
   */
  // Used to give Gradle information about output content
  @Suppress("UNUSED")
  @get:OutputFile
  val destination: Provider<RegularFile> = srcDir.let { dir ->
    dir.file(javaFile.map {
      it.packageName.split(".") + listOf("${it.typeSpec.name}.java")
    }.map { it.joinToString(separator = File.separator) })
  }

  companion object {
    private val LOGGER = KotlinLogging.logger {}
  }

  /**
   * Writes the [JavaFile] to the [srcDir].
   */
  @TaskAction
  fun createFile() {
    val java = javaFile.get()
    val srcDirPath = srcDir.get().asFile.toPath()
    LOGGER.info { "Generating Java file at $ for class ${java.typeSpec.name} in package ${java.packageName} to $srcDirPath" }
    java.writeTo(srcDirPath)
  }
}
