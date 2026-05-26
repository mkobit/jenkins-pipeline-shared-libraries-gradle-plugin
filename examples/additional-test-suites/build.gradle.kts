plugins {
    id("com.mkobit.jenkins.pipelines.shared-library")
}

testing {
    suites {
        named<JvmTestSuite>("test") {
        }
    }
}

// Consumer-defined third suite. The plugin wires `test` and `integrationTest`
// automatically; any additional Jenkins test suite opts in via withJenkins().
val smokeTest = testing.suites.register<JvmTestSuite>("smokeTest") {
    sources {
        java.setSrcDirs(listOf("test/smoke/java"))
    }
    sharedLibrary.withJenkins(this)
}

tasks.check {
    dependsOn(smokeTest)
}
