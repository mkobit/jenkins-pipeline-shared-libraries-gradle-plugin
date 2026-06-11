package testsupport.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner

fun GradleRunner.build(vararg args: String): BuildResult = withArguments(*args).build()

fun GradleRunner.buildAndFail(vararg args: String): BuildResult = withArguments(*args).buildAndFail()
