package com.mkobit.jenkins.pipelines

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.process.CommandLineArgumentProvider

abstract class LibraryRootArgumentProvider : CommandLineArgumentProvider {
  /** Resolved path of the synced shared library source directory (build output). */
  @get:Internal
  abstract val libraryRoot: DirectoryProperty

  override fun asArguments() = listOf("-Dtest.library.root=${libraryRoot.get().asFile.absolutePath}")
}
