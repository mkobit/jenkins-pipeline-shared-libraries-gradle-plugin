package com.mkobit.jenkins.pipelines

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.CommandLineArgumentProvider

abstract class JenkinsWarJvmArgumentProvider : CommandLineArgumentProvider {
  @get:InputFile
  @get:PathSensitive(PathSensitivity.NONE)
  abstract val warFile: RegularFileProperty

  override fun asArguments(): Iterable<String> = listOf("-Djth.jenkins-war.path=${warFile.get().asFile.absolutePath}")
}
