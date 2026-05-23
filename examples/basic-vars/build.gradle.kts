plugins {
    id("com.mkobit.jenkins.pipelines.shared-library")
}

tasks.named("codenarcMain") { enabled = false }
tasks.named("codenarcTest") { enabled = false }

testing {
    suites {
        named<JvmTestSuite>("test") {
            dependencies {
                implementation("org.codehaus.groovy:groovy:2.4.21")
            }
        }
        named<JvmTestSuite>("integrationTest") {
            useJUnit()
        }
    }
}
