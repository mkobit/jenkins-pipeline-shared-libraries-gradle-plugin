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
        // Opt-in peer: pipelines that want notifications must add `@Library('notifier') _`
        // at the top of the Jenkinsfile. Other pipelines won't pay the load cost.
        sharedLibrary("com.example.pipeline:notify-lib:1.0") {
            libraryName = "notifier"
            implicit = false
        }
    }
}
