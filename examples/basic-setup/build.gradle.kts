@file:Suppress("UnstableApiUsage")

import org.gradle.api.plugins.jvm.JvmTestSuite

plugins {
  id("com.mkobit.jenkins.pipelines.shared-library")
}

// All sharedLibrary defaults accepted: Jenkins 2.479.1, auto library registration, library name = "basic-setup".

testing {
  suites {
    named<JvmTestSuite>("integrationTest") {
      useJUnitJupiter("6.0.3")
      dependencies {
        runtimeOnly("org.junit.platform:junit-platform-launcher:6.0.3")
      }
    }
  }
}
