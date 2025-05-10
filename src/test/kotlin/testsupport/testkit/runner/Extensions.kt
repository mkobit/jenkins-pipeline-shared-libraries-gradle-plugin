package testsupport.testkit.runner

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.nio.file.Path

/**
 * The `--info` flag.
 */
var GradleRunner.info: Boolean
  get() = arguments.contains("--info")
  set(value) {
    ensureFlagOptionState("--info", value)
  }

/**
 * The `--stacktrace` flag.
 */
var GradleRunner.stacktrace: Boolean
  get() = arguments.contains("--stacktrace")
  set(value) {
    ensureFlagOptionState("--stacktrace", value)
  }

/**
 * The `--quiet` flag.
 */
var GradleRunner.quiet: Boolean
  get() = arguments.contains("--quiet")
  set(value) {
    ensureFlagOptionState("--quiet", value)
  }

/**
 * Convenient extension method to build the project with the current configuration.
 * Equivalent to calling GradleRunner.build()
 *
 * @param tasks The Gradle tasks to execute
 * @return BuildResult from running the build
 */
fun GradleRunner.build(vararg tasks: String): BuildResult {
  return withArguments(arguments + tasks.toList()).build()
}

/**
 * Convenient extension method to build the project expecting failure.
 * Equivalent to calling GradleRunner.buildAndFail()
 *
 * @param tasks The Gradle tasks to execute
 * @return BuildResult from running the build that failed
 */
fun GradleRunner.buildAndFail(vararg tasks: String): BuildResult {
  return withArguments(arguments + tasks.toList()).buildAndFail()
}

/**
 * Runs a build expected to fail using GradleRunner.buildAndFail().
 *
 * @param tasks The collection of tasks to execute
 * @return BuildResult from running the build that failed
 */
fun GradleRunner.buildAndFail(tasks: Collection<String>): BuildResult {
  return withArguments(arguments + tasks).buildAndFail()
}

/**
 * Updates the [GradleRunner.getArguments] to ensure that the provided [flag] is included or excluded
 * depending on the value of [include].
 * @param flag the flag to ensure is present in the arguments
 * @param include `true` if the [flag] should be included, `false` if it should be removed
 */
private fun GradleRunner.ensureFlagOptionState(
  flag: String,
  include: Boolean
) {
  val currentlyContained = arguments.contains(flag)
  if (include) {
    if (!currentlyContained) {
      withArguments(arguments + listOf(flag))
    }
  } else {
    if (currentlyContained) {
      withArguments(arguments.filter { it != flag })
    }
  }
}

/**
 * Extension property to access the GradleRunner's project directory as a Path.
 */
var GradleRunner.projectDirPath: Path?
  get() = projectDir?.toPath()
  set(value) {
    value?.let { withProjectDir(it.toFile()) }
  }

/**
 * Sets the project directory for the GradleRunner using a Path.
 *
 * @param path The Path to set as the project directory.
 * @return The GradleRunner instance with the project directory set.
 */
fun GradleRunner.withProjectDirPath(path: Path): GradleRunner {
  return withProjectDir(path.toFile())
}

/**
 * Set up the GradleRunner's project directory by performing operations within the context of the directory.
 *
 * @param action A lambda with operations to perform on the project directory.
 * @return The GradleRunner instance for chaining.
 * @throws IllegalStateException if the project directory is not specified
 */
fun GradleRunner.setupProjectDir(action: (Path) -> Unit): GradleRunner =
  apply {
    val path = projectDir?.toPath() ?: throw IllegalStateException("Project directory must be specified")
    action(path)
  }
