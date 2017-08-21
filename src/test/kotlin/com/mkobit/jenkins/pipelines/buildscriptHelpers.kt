package com.mkobit.jenkins.pipelines

internal fun groovyBuildScript(): String = """
plugins {
  id 'com.mkobit.jenkins.pipelines.shared-library'
}

repositories {
  jcenter()
}

dependencies {
  testImplementation(group: 'junit', name: 'junit', version: '4.12')
}
"""

internal fun kotlinBuildScript(): String = """
plugins {
  id("com.mkobit.jenkins.pipelines.shared-library")
}

repositories {
  jcenter()
}

dependencies {
  testImplementation("junit", "junit", "4.12")
}
"""
