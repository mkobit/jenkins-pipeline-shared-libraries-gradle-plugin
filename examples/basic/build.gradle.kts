plugins {
    id("com.mkobit.jenkins.pipelines.shared-library")
}

testing {
    suites {
        named<JvmTestSuite>("test") {
        }
        named<JvmTestSuite>("integrationTest") {
        }
    }
}
