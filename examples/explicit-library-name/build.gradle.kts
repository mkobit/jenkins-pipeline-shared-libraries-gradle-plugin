plugins {
    id("com.mkobit.jenkins.pipelines.shared-library")
}

codenarc {
    configFile = file("config/codenarc/codenarc.xml")
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
