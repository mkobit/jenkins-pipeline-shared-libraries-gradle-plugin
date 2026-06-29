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

val exampleDirs =
    projectDir
        .listFiles { f -> f.isDirectory && f.resolve("settings.gradle.kts").exists() }
        ?.sortedBy { it.name }
        .orEmpty()

val exampleTasks =
    exampleDirs.map { exampleDir ->
        tasks.register<Exec>("example-${exampleDir.name}") {
            group = JavaBasePlugin.VERIFICATION_GROUP
            description = "Runs check for the ${exampleDir.name} example"
            workingDir = exampleDir
            commandLine(gradlew.absolutePath, "check")
            usesService(examplesBuildService)
        }
    }

// Discover nested included builds inside each example (a child directory with its own
// settings.gradle.kts). Computed once per example at configuration time so the resulting
// prune-example-<name> tasks have a stable, CC-safe input.
val pruneExampleTasks =
    exampleDirs.map { exampleDir ->
        val nestedBuildDirs =
            exampleDir
                .listFiles { f -> f.isDirectory && f.resolve("settings.gradle.kts").exists() }
                ?.sortedBy { it.name }
                .orEmpty()
        val stateDirs =
            (listOf(exampleDir) + nestedBuildDirs).flatMap { dir ->
                listOf(dir.resolve("build"), dir.resolve(".gradle"))
            }
        tasks.register<Delete>("prune-example-${exampleDir.name}") {
            group = "Example Maintenance"
            description =
                "Deletes Gradle state (.gradle/, build/) for the ${exampleDir.name} example and any nested included builds"
            delete(stateDirs)
            // If the matching example-<name> run task is in the same task graph, run after it
            // so a single invocation like `:examples:example-foo :examples:prune-example-foo`
            // produces a fresh-then-cleaned sequence. shouldRunAfter is a soft constraint —
            // prune-example-<name> can be invoked standalone without the run task being scheduled.
            shouldRunAfter("example-${exampleDir.name}")
        }
    }

tasks.register("pruneAllExamples") {
    group = "Example Maintenance"
    description = "Prunes Gradle state from every example"
    dependsOn(pruneExampleTasks)
}

tasks.register<GenerateJsonMatrix>("generateExamplesMatrix") {
    group = "CI"
    description = "Writes the examples CI matrix JSON to <build>/ci/examples-matrix.json"
    matrixEntries = exampleDirs.map { MatrixEntry(mapOf("example" to it.name)) }
    outputFile = layout.buildDirectory.dir("ci").map { it.file("examples-matrix.json") }
}

tasks.check {
    dependsOn(exampleTasks)
}
