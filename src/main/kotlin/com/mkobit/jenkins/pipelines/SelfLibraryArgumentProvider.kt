package com.mkobit.jenkins.pipelines

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.CommandLineArgumentProvider

abstract class SelfLibraryArgumentProvider : CommandLineArgumentProvider {
  @get:Internal
  abstract val libraryName: Property<String>

  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val libraryLocation: DirectoryProperty

  @get:Internal
  abstract val implicit: Property<Boolean>

  override fun asArguments() =
    listOf(
      "-Dtest.library.0.name=${libraryName.get()}",
      "-Dtest.library.0.location=${libraryLocation.get().asFile.absolutePath}",
      "-Dtest.library.0.implicit=${implicit.get()}",
    )
}
