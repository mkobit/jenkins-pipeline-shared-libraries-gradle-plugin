package com.mkobit.jenkins.pipelines

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.process.CommandLineArgumentProvider

abstract class LibraryLocationArgumentProvider : CommandLineArgumentProvider {
  /** Absolute path of the synced shared library source directory (sync task output). */
  @get:Internal
  abstract val libraryLocation: DirectoryProperty

  override fun asArguments() = listOf("-Dtest.library.location=${libraryLocation.get().asFile.absolutePath}")
}
