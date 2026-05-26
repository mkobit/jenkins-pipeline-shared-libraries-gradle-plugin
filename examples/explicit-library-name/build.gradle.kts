plugins {
    id("com.mkobit.jenkins.pipelines.shared-library")
}

sharedLibrary {
    libraryName = "my-pipeline-lib"
    implicit = false
}

testing {
    suites {
        named<JvmTestSuite>("integrationTest") {
        }
    }
}
