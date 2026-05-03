package com.mkobit.jenkins.pipelines

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.process.CommandLineArgumentProvider

abstract class JenkinsWarJvmArgumentProvider : CommandLineArgumentProvider {
  // @Internal so Gradle resolves the WAR path at execution time, not during task-graph
  // construction. Resolving at graph-construction time fails in projects without repos.
  @get:Internal
  abstract val warFile: RegularFileProperty

  override fun asArguments(): Iterable<String> = listOf("-Djth.jenkins-war.path=${warFile.get().asFile.absolutePath}")
}
