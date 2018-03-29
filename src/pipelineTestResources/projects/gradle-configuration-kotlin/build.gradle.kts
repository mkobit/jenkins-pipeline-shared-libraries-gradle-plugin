plugins {
  id("com.mkobit.jenkins.pipelines.shared-library")
}

repositories {
  jcenter()
}

dependencies {
  testImplementation("junit:junit:4.12")
}

sharedLibrary {
  coreVersion.set("2.86")
  testHarnessVersion.set("2.32")
  pluginDependencies {
    workflowCpsGlobalLibraryPluginVersion.set("2.9")
    workflowCpsPluginVersion.set("2.4")
    dependency("io.jenkins.blueocean", "blueocean-web", "1.3.0")
  }
}
