import org.gradle.api.plugins.JavaBasePlugin

// Each example task spawns an independent Gradle process. Within that process the plugin
// sequences test → integrationTest (mustRunAfter) and caps the integration test JVM at 2g
// (SharedLibraryDefaults.INTEGRATION_TEST_MAX_HEAP_SIZE). The Gradle daemon for each child
// build adds another JVM on top of that.
//
// With org.gradle.parallel=true in the root build, multiple example-* tasks can run
// concurrently, stacking N×(daemon + 2g test JVM) simultaneously. This is fine for CI
// (separate runners per matrix job) but may be memory-intensive locally.
//
// To investigate memory across all JVMs for a single run, temporarily append "--scan" to
// commandLine(...) below and inspect the build scan timeline.
plugins {
    base
}

val gradlew = rootProject.file("gradlew")

val exampleTasks =
    projectDir
        .listFiles { f -> f.isDirectory && f.resolve("settings.gradle.kts").exists() }
        ?.sortedBy { it.name }
        ?.map { exampleDir ->
            tasks.register<Exec>("example-${exampleDir.name}") {
                group = JavaBasePlugin.VERIFICATION_GROUP
                description = "Runs check for the ${exampleDir.name} example"
                workingDir = exampleDir
                commandLine(gradlew.absolutePath, "check")
            }
        } ?: emptyList()

tasks.check {
    dependsOn(exampleTasks)
}
