plugins {
    id("com.mkobit.jenkins.pipelines.shared-library")
}

sharedLibrary {
    dependencies {
        sharedLibrary("com.example.pipeline:deployer:1.0") {
            libraryName = "pipeline-deployer"
        }
        // Opt-in peer: pipelines must add `@Library('pipeline-notifier') _` to use it.
        sharedLibrary("com.example.pipeline:notifier:1.0") {
            libraryName = "pipeline-notifier"
            implicit = false
        }
    }
}
