import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

// Each example task spawns an independent Gradle process. Within that process the plugin
// sequences test → integrationTest (mustRunAfter) and caps the integration test JVM at 2g
// (SharedLibraryDefaults.INTEGRATION_TEST_MAX_HEAP_SIZE). The Gradle daemon for each child
// build adds another JVM on top of that.
//
// ExamplesBuildService shares the build-wide "heavyTest" slot with functional tests in
// build.gradle.kts so all memory-intensive Jenkins child processes compete for the same
// concurrency limit. Default is 1 (safe on any machine/container); override with
// -PmemBound.maxParallel=N.
//
// To investigate memory across all JVMs for a single run, temporarily append "--scan" to
// commandLine(...) below and inspect the build scan timeline.
plugins {
    base
}

abstract class ExamplesBuildService : BuildService<BuildServiceParameters.None>

val gradlew = rootProject.file("gradlew")

val examplesBuildService =
    gradle.sharedServices.registerIfAbsent("heavyTest", ExamplesBuildService::class.java) {
        maxParallelUsages =
            providers.gradleProperty("memBound.maxParallel")
                .map { it.toInt() }
                .orElse(1)
    }

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
                usesService(examplesBuildService)
            }
        } ?: emptyList()

tasks.check {
    dependsOn(exampleTasks)
}
