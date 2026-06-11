plugins {
    id("com.mkobit.jenkins.pipelines.shared-library")
}

sharedLibrary {
    dependencies {
        sharedLibrary("com.example.pipeline:deployer:1.0") {
            libraryName = "pipeline-deployer"
        }
        sharedLibrary("com.example.pipeline:notifier:1.0") {
            libraryName = "pipeline-notifier"
        }
    }
}
