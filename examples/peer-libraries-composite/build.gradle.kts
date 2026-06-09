plugins {
    id("com.mkobit.jenkins.pipelines.shared-library")
}

sharedLibrary {
    dependencies {
        sharedLibrary("com.example.pipeline:library-a:1.0")
        sharedLibrary("com.example.pipeline:library-b:1.0")
    }
}
