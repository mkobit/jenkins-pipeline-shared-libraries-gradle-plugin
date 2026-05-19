package com.mkobit.jenkins.pipelines

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.CommandLineArgumentProvider

abstract class LibraryLocationArgumentProvider : CommandLineArgumentProvider {
  // Sync task output; absolute path injected at execution time as -Dtest.library.location.
  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val libraryLocation: DirectoryProperty

  override fun asArguments() = listOf("-Dtest.library.location=${libraryLocation.get().asFile.absolutePath}")
}
