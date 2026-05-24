package com.mkobit.jenkins.pipelines

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.process.CommandLineArgumentProvider

abstract class LibraryImplicitArgumentProvider : CommandLineArgumentProvider {
  @get:Internal
  abstract val implicit: Property<Boolean>

  override fun asArguments() = listOf("-Dtest.library.0.implicit=${implicit.get()}")
}
