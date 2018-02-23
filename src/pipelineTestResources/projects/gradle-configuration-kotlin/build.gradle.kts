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
  groovyVersion = "2.4.12"
  coreVersion = "2.86"
  testHarnessVersion = "2.32"
  pluginDependencies {
    workflowCpsGlobalLibraryPluginVersion = "2.9"
    dependency("io.jenkins.blueocean", "blueocean-web", "1.3.0")
  }
}
