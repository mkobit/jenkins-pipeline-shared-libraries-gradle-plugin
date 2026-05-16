package com.mkobit.jenkins.pipelines

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.process.CommandLineArgumentProvider

abstract class JenkinsWarJvmArgumentProvider : CommandLineArgumentProvider {
  // @Internal rather than @InputFile: Gradle resolves @InputFile properties on
  // CommandLineArgumentProvider at task-graph construction time (including --dry-run),
  // which triggers jenkinsWar configuration resolution before repositories are available
  // in inner TestKit builds. Version-change invalidation still works because asArguments()
  // embeds the absolute WAR path (including version) in the JVM argument string.
  @get:Internal
  abstract val warFile: RegularFileProperty

  override fun asArguments(): Iterable<String> = listOf("-Djth.jenkins-war.path=${warFile.get().asFile.absolutePath}")
}
