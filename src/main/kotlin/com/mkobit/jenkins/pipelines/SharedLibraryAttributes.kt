@file:JvmName("SharedLibraryAttributes")

package com.mkobit.jenkins.pipelines

import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage

/**
 * Value of the [Category] and [Usage] attributes on the `sharedLibrarySourceElements` outgoing variant.
 *
 * The variant exposes the project's `src/`, `vars/`, and `resources/` directories as a
 * directory artifact that can be used to register the library in `JenkinsRule` integration tests.
 *
 * Consumers resolving shared library source from a Gradle dependency must request both attributes:
 *
 * Kotlin (build script):
 * ```kotlin
 * import com.mkobit.jenkins.pipelines.SHARED_LIBRARY_SOURCE_CATEGORY
 * import com.mkobit.jenkins.pipelines.SHARED_LIBRARY_SOURCE_USAGE
 *
 * configurations.create("sharedLibrarySourceConsumer") {
 *     isCanBeResolved = true
 *     isCanBeConsumed = false
 *     attributes {
 *         attribute(Category.CATEGORY_ATTRIBUTE, objects.named<Category>(SHARED_LIBRARY_SOURCE_CATEGORY))
 *         attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>(SHARED_LIBRARY_SOURCE_USAGE))
 *     }
 * }
 * ```
 *
 * Java / Groovy:
 * ```groovy
 * import com.mkobit.jenkins.pipelines.SharedLibraryAttributes
 * import org.gradle.api.attributes.Category
 * import org.gradle.api.attributes.Usage
 *
 * configurations.create("sharedLibrarySourceConsumer") {
 *     canBeResolved = true
 *     canBeConsumed = false
 *     attributes {
 *         attribute(Category.CATEGORY_ATTRIBUTE,
 *             objects.named(Category, SharedLibraryAttributes.SHARED_LIBRARY_SOURCE_CATEGORY))
 *         attribute(Usage.USAGE_ATTRIBUTE,
 *             objects.named(Usage, SharedLibraryAttributes.SHARED_LIBRARY_SOURCE_USAGE))
 *     }
 * }
 * ```
 */
const val SHARED_LIBRARY_SOURCE_CATEGORY = "jenkins-shared-library"

/** Value of the [Usage] attribute on the `sharedLibrarySourceElements` outgoing variant. */
const val SHARED_LIBRARY_SOURCE_USAGE = "jenkins-shared-library-source"
