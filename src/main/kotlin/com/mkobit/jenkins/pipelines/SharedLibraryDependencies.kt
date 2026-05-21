@file:Suppress("UnstableApiUsage")

package com.mkobit.jenkins.pipelines

import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.Dependencies
import org.gradle.api.artifacts.dsl.DependencyCollector
import org.gradle.api.provider.Provider
import javax.inject.Inject

/**
 * Dependency block for declaring peer Jenkins shared library dependencies inside `sharedLibrary {}`.
 *
 * Each declared peer library participates in two resolutions:
 * 1. As a compiled JAR on the consumer's `compileOnly` and test suite `implementation` configurations
 *    — for Groovy/IDE symbol resolution against types defined in the peer library.
 * 2. As a `sharedLibrarySourceElements` directory artefact — injected into Jenkins at integration-test
 *    runtime via `test.library.N.location` so the peer library's `src/`, `vars/`, and `resources/`
 *    are available to pipelines without manual `GlobalLibraries` wiring.
 *
 * ```kotlin
 * sharedLibrary {
 *   dependencies {
 *     sharedLibrary("com.example:config-lib:1.0.0")
 *     sharedLibrary(project(":config-lib"))
 *   }
 * }
 * ```
 */
abstract class SharedLibraryDependencies
  @Inject
  constructor() : Dependencies {
    abstract val sharedLibraryCollector: DependencyCollector

    /** Declares a peer shared library by `"group:artifact"` or `"group:artifact:version"` notation. */
    fun sharedLibrary(notation: CharSequence) = sharedLibraryCollector.add(notation)

    /** Declares a peer shared library from a version catalog entry. */
    fun sharedLibrary(dependency: Provider<out MinimalExternalModuleDependency>) = sharedLibraryCollector.add(dependency)

    /** Declares a peer shared library from another project in this build (multi-project or composite). */
    fun sharedLibrary(project: ProjectDependency) = sharedLibraryCollector.add(project)
  }
