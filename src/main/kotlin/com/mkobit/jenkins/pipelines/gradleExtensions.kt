package com.mkobit.jenkins.pipelines

import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.HasConvention
import org.gradle.api.provider.PropertyState
import org.gradle.api.tasks.GroovySourceSet
import org.gradle.api.tasks.SourceSet


inline internal fun <reified T> Project.initializedProperty(initialState: T): PropertyState<T> = this.property(T::class.java).apply {
  set(initialState)
}

