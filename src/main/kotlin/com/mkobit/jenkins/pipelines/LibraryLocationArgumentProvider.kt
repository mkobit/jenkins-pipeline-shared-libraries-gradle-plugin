package com.mkobit.jenkins.pipelines

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.CommandLineArgumentProvider

abstract class LibraryLocationArgumentProvider : CommandLineArgumentProvider {
  /** Absolute path of the synced shared library source directory (sync task output). */
  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.ABSOLUTE)
  abstract val libraryLocation: DirectoryProperty

  override fun asArguments() = listOf("-Dtest.library.location=${libraryLocation.get().asFile.absolutePath}")
}
