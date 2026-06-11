plugins {
    id("com.mkobit.jenkins.pipelines.shared-library")
}

sharedLibrary {
    dependencies {
        sharedLibrary(project(":deploy-lib")) {
            libraryName = "deployer"
        }
        sharedLibrary(project(":checks-lib")) {
            libraryName = "pre-checks"
        }
        sharedLibrary("com.example.pipeline:notify-lib:1.0") {
            libraryName = "notifier"
        }
    }
}
