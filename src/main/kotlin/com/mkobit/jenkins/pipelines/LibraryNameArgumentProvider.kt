package com.mkobit.jenkins.pipelines

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.process.CommandLineArgumentProvider

abstract class LibraryNameArgumentProvider : CommandLineArgumentProvider {
  @get:Internal
  abstract val libraryName: Property<String>

  override fun asArguments() = listOf("-Dtest.library.name=${libraryName.get()}")
}
