@file:Suppress("UnstableApiUsage")

package com.mkobit.jenkins.pipelines

import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.dsl.Dependencies
import org.gradle.api.artifacts.dsl.DependencyCollector
import org.gradle.api.provider.Provider
import javax.inject.Inject

/**
 * Dependency block for declaring Jenkins HPI/JPI plugin dependencies inside `sharedLibrary {}`.
 *
 * ```kotlin
 * sharedLibrary {
 *   plugins {
 *     plugin("org.jenkins-ci.plugins.workflow:workflow-multibranch")
 *     plugin("org.6wind.jenkins:lockable-resources:2.18")
 *     plugin(libs.workflow.multibranch)           // single version catalog entry
 *     plugins(libs.bundles.allPlugins)            // version catalog bundle
 *   }
 * }
 * ```
 */
abstract class JenkinsPlugins
  @Inject
  constructor() : Dependencies {
    abstract val pluginCollector: DependencyCollector

    /** Declares a Jenkins plugin by `"group:artifact"` or `"group:artifact:version"` notation. */
    fun plugin(notation: CharSequence) = pluginCollector.add(notation)

    /** Declares a Jenkins plugin from a version catalog entry. */
    fun plugin(dependency: Provider<out MinimalExternalModuleDependency>) = pluginCollector.add(dependency)

    /** Declares all Jenkins plugins from a version catalog bundle (e.g. `jenkinsPlugins.bundles.allPlugins`). */
    fun plugins(bundle: Provider<out Iterable<MinimalExternalModuleDependency>>) = pluginCollector.bundle(bundle)
  }
