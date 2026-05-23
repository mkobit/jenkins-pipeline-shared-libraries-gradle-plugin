plugins {
    id("com.mkobit.jenkins.pipelines.shared-library")
}

// TODO(#173): remove once codenarcMain is opt-in or ships a default config
// https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/173
tasks.named("codenarcMain") { enabled = false }
tasks.named("codenarcTest") { enabled = false }

testing {
    suites {
        named<JvmTestSuite>("test") {
            dependencies {
                // TODO(#161): remove once the plugin auto-adds groovy alongside jenkins-pipeline-unit
                // https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/161
                implementation("org.codehaus.groovy:groovy:2.4.21")
            }
        }
        named<JvmTestSuite>("integrationTest") {
        }
    }
}
