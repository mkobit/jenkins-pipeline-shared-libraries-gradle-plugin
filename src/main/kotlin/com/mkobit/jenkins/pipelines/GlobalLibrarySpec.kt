package com.mkobit.jenkins.pipelines

import org.gradle.api.Named
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Declares an external Jenkins shared library to auto-register alongside the project's own library.
 *
 * Each spec maps to one `@Library` declaration consumers can reference in pipeline scripts.
 * The name is the Jenkins library name (the string passed to `@Library('name')`).
 *
 * ```kotlin
 * sharedLibrary {
 *     globalLibraries {
 *         create("other-lib") {
 *             implicit = false   // explicit @Library required in pipeline scripts
 *             defaultVersion = "fixed"
 *         }
 *     }
 * }
 * ```
 *
 * **Source resolution is not yet implemented.** Declaring a `GlobalLibrarySpec` reserves the
 * name and configuration for future wiring — the library source path must currently be provided
 * by other means. The API is defined now so consumer build scripts do not need to change when
 * resolution support (via a Gradle dependency coordinate) is added.
 */
internal abstract class GlobalLibrarySpec
  @Inject
  constructor(private val libraryName: String) : Named {
    override fun getName(): String = libraryName

    /**
     * When `true`, the library is implicitly loaded into every pipeline execution without an explicit
     * `@Library` declaration. Defaults to `false` for external libraries (unlike the project's own
     * library, which defaults to implicit).
     */
    abstract val implicit: Property<Boolean>

    /**
     * Default version string passed to `LibraryConfiguration`. Typically `"fixed"` for
     * directory-backed retrievers (the library version is pinned, not resolved from SCM).
     */
    abstract val defaultVersion: Property<String>
  }
