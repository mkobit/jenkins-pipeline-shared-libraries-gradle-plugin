package com.mkobit.jenkins.pipelines

import org.gradle.api.Project
import org.gradle.api.provider.Property

internal inline fun <reified T> Project.initializedProperty(initialState: T): Property<T> =
  this.objects.property(T::class.java).apply {
    set(initialState)
  }
