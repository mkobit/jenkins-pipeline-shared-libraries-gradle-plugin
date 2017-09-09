package com.mkobit.jenkins.pipelines

internal fun kotlinBuildScript(): String = """
plugins {
  id("com.mkobit.jenkins.pipelines.shared-library")
}

repositories {
  jcenter()
}

dependencies {
  testImplementation("junit:junit:4.12")
}
"""
