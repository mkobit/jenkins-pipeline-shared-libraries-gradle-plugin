plugins {
    id("com.mkobit.jenkins.pipelines.shared-library")
}

testing {
    suites {
        named<JvmTestSuite>("test") {
            dependencies {
                // TODO(#161): remove once the plugin auto-adds groovy alongside jenkins-pipeline-unit
                // https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/161
                implementation("org.codehaus.groovy:groovy:2.4.21")
            }
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
