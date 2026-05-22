@file:Suppress("UnstableApiUsage")

package com.mkobit.jenkins.pipelines

import org.gradle.api.Action
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.Dependencies
import org.gradle.api.artifacts.dsl.DependencyCollector
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.newInstance
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
 *     sharedLibrary("com.example:config-lib:1.0.0") {
 *       libraryName.set("config")     // override the Jenkins library name
 *       implicit.set(false)            // require @Library('config') _ in pipelines
 *     }
 *   }
 * }
 * ```
 */
abstract class SharedLibraryDependencies
  @Inject
  constructor(
    private val objects: ObjectFactory,
  ) : Dependencies {
    /** Feeds the `sharedLibraryDependencies` bucket configuration used for classpath wiring. */
    abstract val sharedLibraryCollector: DependencyCollector

    /**
     * Per-declaration metadata used at integration-test injection time to assign Jenkins library
     * names (default: artifact / project name) and the implicit flag (default: `true`).
     */
    abstract val specs: ListProperty<PeerLibrarySpec>

    /** Declares a peer shared library by `"group:artifact:version"` notation. */
    fun sharedLibrary(notation: CharSequence): Unit = addModuleSpec(notation) { }

    /** Declares a peer shared library by GAV notation and applies per-library overrides. */
    fun sharedLibrary(
      notation: CharSequence,
      action: Action<in PeerLibrarySpec>,
    ): Unit = addModuleSpec(notation, action)

    /** Declares a peer shared library from a version catalog entry. */
    fun sharedLibrary(dependency: Provider<out MinimalExternalModuleDependency>): Unit = addProviderSpec(dependency) { }

    /** Declares a peer shared library from a version catalog entry and applies per-library overrides. */
    fun sharedLibrary(
      dependency: Provider<out MinimalExternalModuleDependency>,
      action: Action<in PeerLibrarySpec>,
    ): Unit = addProviderSpec(dependency, action)

    /** Declares a peer shared library from another project in this build (multi-project or composite). */
    fun sharedLibrary(project: ProjectDependency): Unit = addProjectSpec(project) { }

    /** Declares a peer shared library from another project and applies per-library overrides. */
    fun sharedLibrary(
      project: ProjectDependency,
      action: Action<in PeerLibrarySpec>,
    ): Unit = addProjectSpec(project, action)

    private fun addModuleSpec(
      notation: CharSequence,
      action: Action<in PeerLibrarySpec>,
    ) {
      val coords = notation.toString()
      sharedLibraryCollector.add(coords)
      val parts = coords.split(':')
      val group = parts.getOrNull(0).orEmpty()
      val artifact = parts.getOrNull(1).orEmpty()
      val spec = objects.newInstance<PeerLibrarySpec>()
      spec.identifier.set("$group:$artifact")
      spec.libraryName.convention(artifact.ifEmpty { coords })
      spec.implicit.convention(true)
      action.execute(spec)
      specs.add(spec)
    }

    private fun addProviderSpec(
      dependency: Provider<out MinimalExternalModuleDependency>,
      action: Action<in PeerLibrarySpec>,
    ) {
      sharedLibraryCollector.add(dependency)
      val spec = objects.newInstance<PeerLibrarySpec>()
      spec.identifier.set(dependency.map { "${it.module.group}:${it.module.name}" })
      spec.libraryName.convention(dependency.map { it.module.name })
      spec.implicit.convention(true)
      action.execute(spec)
      specs.add(spec)
    }

    private fun addProjectSpec(
      project: ProjectDependency,
      action: Action<in PeerLibrarySpec>,
    ) {
      sharedLibraryCollector.add(project)
      val spec = objects.newInstance<PeerLibrarySpec>()
      spec.identifier.set(project.path)
      spec.libraryName.convention(project.name)
      spec.implicit.convention(true)
      action.execute(spec)
      specs.add(spec)
    }
  }
