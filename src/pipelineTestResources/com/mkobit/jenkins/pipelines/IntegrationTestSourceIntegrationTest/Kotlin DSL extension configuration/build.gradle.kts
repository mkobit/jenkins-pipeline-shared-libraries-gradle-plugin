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
  coreVersion = "2.73"
  pipelineTestUnitVersion = "1.1"
  testHarnessVersion = "2.24"
  pluginDependencies {
    workflowCpsGlobalLibraryPluginVersion = "2.8"
    blueocean("blueocean-web", "1.2.0")
  }
}
