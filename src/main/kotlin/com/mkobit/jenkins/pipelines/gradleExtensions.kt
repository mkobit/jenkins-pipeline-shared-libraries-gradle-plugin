package com.mkobit.jenkins.pipelines

import org.gradle.api.Project
import org.gradle.api.provider.Property

inline internal fun <reified T> Project.initializedProperty(initialState: T): Property<T> = this.objects.property(T::class.java).apply {
  set(initialState)
}
