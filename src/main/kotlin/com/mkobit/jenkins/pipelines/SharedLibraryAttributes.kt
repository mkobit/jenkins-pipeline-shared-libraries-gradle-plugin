@file:JvmName("SharedLibraryAttributes")

package com.mkobit.jenkins.pipelines

import org.gradle.api.attributes.Category

/**
 * Value of the [Category] attribute on the `sharedLibrarySourceElements` outgoing variant.
 *
 * The variant exposes the project's `src/`, `vars/`, and `resources/` directories as a
 * directory artifact that can be used to register the library in `JenkinsRule` integration tests.
 *
 * Consumers resolving shared library source from a Gradle dependency must request this attribute:
 *
 * Kotlin (build script):
 * ```kotlin
 * import com.mkobit.jenkins.pipelines.SHARED_LIBRARY_SOURCE_CATEGORY
 *
 * configurations.create("sharedLibrarySourceConsumer") {
 *     isCanBeResolved = true
 *     isCanBeConsumed = false
 *     attributes {
 *         attribute(Category.CATEGORY_ATTRIBUTE, objects.named<Category>(SHARED_LIBRARY_SOURCE_CATEGORY))
 *     }
 * }
 * ```
 *
 * Java / Groovy:
 * ```groovy
 * configurations.create("sharedLibrarySourceConsumer") {
 *     canBeResolved = true
 *     canBeConsumed = false
 *     attributes {
 *         attribute(Category.CATEGORY_ATTRIBUTE,
 *             objects.named(Category, SharedLibraryAttributes.SHARED_LIBRARY_SOURCE_CATEGORY))
 *     }
 * }
 * ```
 */
const val SHARED_LIBRARY_SOURCE_CATEGORY = "shared-library-source"
