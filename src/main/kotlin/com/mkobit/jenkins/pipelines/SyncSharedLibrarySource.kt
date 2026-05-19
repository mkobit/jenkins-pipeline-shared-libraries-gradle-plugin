package com.mkobit.jenkins.pipelines

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Copies `src/`, `vars/`, and `resources/` into the declared [destinationDir].
 *
 * Encapsulates the sync spec so external callers cannot alter which directories
 * are included or how they are laid out — the shared library source layout is fixed.
 */
@CacheableTask
abstract class SyncSharedLibrarySource : DefaultTask() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val srcFiles: ConfigurableFileCollection

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val varsFiles: ConfigurableFileCollection

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val resourcesFiles: ConfigurableFileCollection

  @get:OutputDirectory
  abstract val destinationDir: DirectoryProperty

  @TaskAction
  fun sync() {
    project.sync {
      from(srcFiles) { into("src") }
      from(varsFiles) { into("vars") }
      from(resourcesFiles) { into("resources") }
      into(destinationDir.get())
    }
  }
}
