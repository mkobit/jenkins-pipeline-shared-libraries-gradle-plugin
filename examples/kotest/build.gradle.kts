plugins {
    id("com.mkobit.jenkins.pipelines.shared-library")
    kotlin("jvm") version "2.1.21"
}

// TODO(#173): remove once codenarcMain is opt-in or ships a default config
// https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/173
tasks.named("codenarcMain") { enabled = false }
tasks.named("codenarcTest") { enabled = false }

testing {
    suites {
        named<JvmTestSuite>("test") {
            sources {
                kotlin.setSrcDirs(listOf("test/unit/kotlin"))
            }
            dependencies {
                // TODO(#161): remove once the plugin auto-adds groovy alongside jenkins-pipeline-unit
                // https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/161
                implementation("org.codehaus.groovy:groovy:2.4.21")
                implementation(platform("io.kotest:kotest-bom:6.1.11"))
                implementation("io.kotest:kotest-framework-engine")
                implementation("io.kotest:kotest-assertions-core")
                implementation("io.kotest:kotest-extensions-decoroutinator")
                runtimeOnly("io.kotest:kotest-runner-junit5")
            }
        }
        named<JvmTestSuite>("integrationTest") {
            useJUnitJupiter()
            sources {
                kotlin.setSrcDirs(listOf("test/integration/kotlin"))
            }
            dependencies {
                implementation(platform("io.kotest:kotest-bom:6.1.11"))
                implementation("io.kotest:kotest-framework-engine")
                implementation("io.kotest:kotest-assertions-core")
                implementation("io.kotest:kotest-extensions-decoroutinator")
                runtimeOnly("io.kotest:kotest-runner-junit5")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
            }
        }
    }
}
