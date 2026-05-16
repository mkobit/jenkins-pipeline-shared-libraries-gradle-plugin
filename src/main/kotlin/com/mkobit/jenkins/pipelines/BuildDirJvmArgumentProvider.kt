package com.mkobit.jenkins.pipelines

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.process.CommandLineArgumentProvider

abstract class BuildDirJvmArgumentProvider : CommandLineArgumentProvider {
  // @Internal: WarExploder uses buildDirectory only as an output base; the actual
  // outputs are tracked via outputs.dir(suiteJenkinsDir) on the test task.
  @get:Internal
  abstract val dir: DirectoryProperty

  override fun asArguments(): Iterable<String> = listOf("-DbuildDirectory=${dir.get().asFile.absolutePath}")
}
