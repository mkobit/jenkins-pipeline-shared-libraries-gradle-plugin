package com.mkobit.jenkins.pipelines

import org.gradle.api.attributes.Category

/**
 * Gradle variant attribute values used by the shared library source outgoing variant.
 *
 * Consumers that want to resolve shared library source from a dependency (e.g. to register
 * an external `@Library` in integration tests) must request these attributes when creating
 * a resolvable configuration:
 *
 * ```kotlin
 * configurations.create("sharedLibrarySourceConsumer") {
 *     isCanBeResolved = true
 *     isCanBeConsumed = false
 *     attributes {
 *         attribute(
 *             Category.CATEGORY_ATTRIBUTE,
 *             objects.named(Category::class.java, SharedLibraryAttributes.SHARED_LIBRARY_SOURCE_CATEGORY),
 *         )
 *     }
 * }
 * ```
 */
object SharedLibraryAttributes {
  /**
   * Value of the [Category] attribute on the `sharedLibrarySourceElements` outgoing variant.
   * The variant exposes the project's `src/`, `vars/`, and `resources/` directories as a
   * directory artifact that can be used to register the library in `JenkinsRule` integration tests.
   */
  const val SHARED_LIBRARY_SOURCE_CATEGORY = "shared-library-source"
}
