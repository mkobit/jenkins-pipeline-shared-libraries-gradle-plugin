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

// For each example, the list of Gradle build directories — the example root plus any
// nested included build (immediate child directory containing its own settings.gradle.kts).
// Each entry independently consumes the shared-library plugin, so the gradle.properties
// baseline (and prune behavior) needs to apply to all of them.
val exampleBuildDirs: Map<File, List<File>> =
    exampleDirs.associateWith { exampleDir ->
        val nested =
            exampleDir
                .listFiles { f -> f.isDirectory && f.resolve("settings.gradle.kts").exists() }
                ?.sortedBy { it.name }
                .orEmpty()
        listOf(exampleDir) + nested
    }

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

val pruneExampleTasks =
    exampleDirs.map { exampleDir ->
        val stateDirs =
            exampleBuildDirs.getValue(exampleDir).flatMap { dir ->
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

// Baseline gradle.properties keys every example must declare so CI exercises the
// plugin under configuration cache and surfaces Gradle deprecations.
// Adding a new key here will fail validateExampleSettings until every example is updated.
val requiredExamplePropertyKeys =
    mapOf(
        "org.gradle.configuration-cache" to "true",
        "org.gradle.java.installations.auto-download" to "false",
        "org.gradle.warning.mode" to "fail",
    )

val validateExampleSettings =
    tasks.register("validateExampleSettings") {
        group = JavaBasePlugin.VERIFICATION_GROUP
        description =
            "Asserts every example and its nested included builds declare the required gradle.properties baseline"
        val expectations = requiredExamplePropertyKeys
        // Map each build directory to its display label (relative to examples/) and its properties file.
        // Top-level example: "basic" -> examples/basic/gradle.properties
        // Nested include:    "peer-libraries/notify-lib" -> examples/peer-libraries/notify-lib/gradle.properties
        val examplesRoot = projectDir
        val propertyFiles =
            exampleBuildDirs.values.flatten().associate { dir ->
                dir.relativeTo(examplesRoot).path to dir.resolve("gradle.properties")
            }
        inputs.files(propertyFiles.values).optional(true)
        inputs.property("expected", expectations)
        doLast {
            val problems = mutableListOf<String>()
            propertyFiles.forEach { (label, file) ->
                if (!file.exists()) {
                    problems += "$label: missing gradle.properties (required for example baseline)"
                    return@forEach
                }
                val parsed = java.util.Properties().apply { file.reader().use { load(it) } }
                expectations.forEach { (key, expected) ->
                    val actual = parsed.getProperty(key)
                    if (actual != expected) {
                        problems +=
                            "$label: gradle.properties[$key] expected \"$expected\", got \"${actual ?: "<absent>"}\""
                    }
                }
            }
            if (problems.isNotEmpty()) {
                throw GradleException(
                    "Example settings baseline violation:\n  " + problems.joinToString("\n  "),
                )
            }
        }
    }

tasks.check {
    dependsOn(exampleTasks, validateExampleSettings)
}
