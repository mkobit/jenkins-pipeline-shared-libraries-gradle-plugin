plugins {
    id("com.mkobit.jenkins.pipelines.shared-library")
}

group = "com.example.pipeline"
version = "1.0"

sharedLibrary {
    dependencies {
        sharedLibrary("com.example.pipeline:version-utils:1.0")
    }
}
