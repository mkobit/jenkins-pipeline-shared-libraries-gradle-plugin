import com.mkobit.jenkins.pipelines.jenkins

plugins {
    id("com.mkobit.jenkins.pipelines.shared-library")
}

// `integrationTest` is wired automatically by the plugin.
// Any additional Jenkins suite opts in with `jenkins.enabled = true`.
val smokeTest = testing.suites.register<JvmTestSuite>("smokeTest") {
    sources {
        java.setSrcDirs(listOf("test/smoke/java"))
    }
    jenkins.enabled = true
}

tasks.check {
    dependsOn(smokeTest)
}
