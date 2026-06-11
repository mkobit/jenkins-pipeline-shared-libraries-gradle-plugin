package com.mkobit.jenkins.pipelines

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import javax.inject.Inject

/**
 * Per-declaration metadata for a peer shared library declared in
 * `sharedLibrary { dependencies { sharedLibrary(...) } }`.
 *
 * Captures overrides the consumer applied via the DSL action block; values not overridden
 * fall back to conventions derived from the dependency's coordinates (artifact name) or
 * project path (project name).
 *
 * ```kotlin
 * sharedLibrary {
 *   dependencies {
 *     sharedLibrary("com.example:config-lib:1.0.0") {
 *       libraryName.set("config")
 *       implicit.set(false)
 *     }
 *   }
 * }
 * ```
 */
abstract class PeerLibrarySpec
  @Inject
  constructor() {
    /**
     * Stable identifier used to correlate this spec with resolved artifacts.
     *
     * Format: `":projectPath"` for project dependencies, `"group:artifact"` for external coordinates.
     * Set by the DSL; not consumer-configurable.
     */
    @get:Internal
    abstract val identifier: Property<String>

    /**
     * Name registered in Jenkins' `GlobalLibraries` for this peer library.
     *
     * Defaults to the artifact name (for GAV deps) or the project name (for project deps).
     * Override when test pipelines reference the library by a name that differs from the
     * Gradle artifact/project identity.
     */
    abstract val libraryName: Property<String>

    /**
     * When `true` (default, mirrors the self-library), the peer library is loaded into every
     * pipeline without requiring `@Library('name') _`.
     */
    abstract val implicit: Property<Boolean>
  }
