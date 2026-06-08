import com.mkobit.jenkins.pipelines.jenkins

plugins {
    id("com.mkobit.jenkins.pipelines.shared-library")
}

// `integrationTest` is wired automatically by the plugin.
// Split tests across two targets so each boots an isolated embedded Jenkins.
testing {
    suites {
        named<JvmTestSuite>("integrationTest") {
            targets {
                named("integrationTest") {
                    testTask.configure { include("**/SayHelloStepTest.class") }
                }
                register("integrationTestGreeter") {
                    testTask.configure { include("**/CpsClassUsageTest.class") }
                    tasks.check { dependsOn(testTask) }
                }
            }
        }
        register<JvmTestSuite>("smokeTest") {
            sources {
                java.setSrcDirs(listOf("test/smoke/java"))
            }
            jenkins.enabled = true
            targets {
                named("smokeTest") {
                    testTask.configure { include("**/SayHelloSmokeTest.class") }
                    tasks.check { dependsOn(testTask) }
                }
                register("smokeTestChain") {
                    testTask.configure { include("**/SayHelloChainSmokeTest.class") }
                    tasks.check { dependsOn(testTask) }
                }
            }
        }
    }
}
