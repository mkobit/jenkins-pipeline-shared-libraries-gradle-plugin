plugins {
    id("com.mkobit.jenkins.pipelines.shared-library")
}

testing {
    suites {
        named<JvmTestSuite>("integrationTest") {
            useSpock("2.3-groovy-3.0")
            sources {
                groovy.setSrcDirs(listOf("test/integration/groovy"))
            }
            dependencies {
                // JenkinsRule uses JUnit 4 @Rule; spock-junit4 wires that into Spock 2.x.
                implementation("org.spockframework:spock-junit4:2.3-groovy-3.0")
            }
        }
    }
}
