package com.mkobit.jenkins.pipelines

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.process.CommandLineArgumentProvider

abstract class JenkinsWarJvmArgumentProvider : CommandLineArgumentProvider {
  // @Internal: Gradle resolves @InputFile properties on CommandLineArgumentProvider instances
  // at task-graph construction time (including --dry-run), which fails in projects without
  // repositories. Version-change invalidation still works because asArguments() embeds the
  // absolute WAR path (which includes the version number) in the JVM argument string; Gradle
  // tracks the full argument list as part of the test task's input snapshot.
  @get:Internal
  abstract val warFile: RegularFileProperty

  override fun asArguments(): Iterable<String> = listOf("-Djth.jenkins-war.path=${warFile.get().asFile.absolutePath}")
}
