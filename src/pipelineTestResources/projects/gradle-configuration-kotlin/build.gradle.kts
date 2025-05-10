plugins {
  id("com.mkobit.jenkins.pipelines.shared-library")
}

  repositories {
    mavenCentral()
  }

  dependencies {
    testImplementation("junit:junit:4.12")
  }

  sharedLibrary {
    coreVersion.set("2.86")
    testHarnessVersion.set("2.32")
    // Cast needed due to https://github.com/gradle/kotlin-dsl/issues/522
    pluginDependencies(
      Action {
        workflowCpsGlobalLibraryPluginVersion.set("2.9")
        workflowCpsPluginVersion.set("2.4")
        dependency("io.jenkins.blueocean", "blueocean-web", "1.3.0")
      }
    )
  }
