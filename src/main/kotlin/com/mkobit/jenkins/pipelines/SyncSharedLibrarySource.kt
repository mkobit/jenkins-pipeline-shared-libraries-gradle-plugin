package com.mkobit.jenkins.pipelines

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.Sync

/**
 * Copies `src/`, `vars/`, and `resources/` into `build/sharedLibrarySource/{libraryName}/`.
 *
 * Annotated `@CacheableTask` so the output can be restored from the build cache.
 * The source override re-declares `@PathSensitive(RELATIVE)` — required for cacheability.
 */
@CacheableTask
abstract class SyncSharedLibrarySource : Sync() {
  @InputFiles
  @PathSensitive(PathSensitivity.RELATIVE)
  override fun getSource(): FileCollection = super.getSource()
}
