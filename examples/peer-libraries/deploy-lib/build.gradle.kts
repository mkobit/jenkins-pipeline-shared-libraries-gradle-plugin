plugins {
    id("com.mkobit.jenkins.pipelines.shared-library")
}

sharedLibrary {
    dependencies {
        sharedLibrary(project(":shell-lib")) {
            libraryName = "shell-utils"
        }
        sharedLibrary(project(":metrics-lib"))
    }
}

