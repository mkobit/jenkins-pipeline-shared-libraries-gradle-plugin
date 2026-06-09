plugins {
    id("com.mkobit.jenkins.pipelines.shared-library")
}

sharedLibrary {
    dependencies {
        sharedLibrary(project(":peer-lib")) {
            libraryName.set("deploy-config")
        }
    }
}
