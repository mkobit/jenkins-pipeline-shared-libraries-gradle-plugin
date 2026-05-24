package testsupport.jenkins.testharness

import hudson.model.Run
import org.jvnet.hudson.test.JenkinsRule

/**
 * Gets the log of a [Run] as a [String].
 * This allows using standard Kotlin assertions (like Kotest's `shouldContain`) on the log output.
 * Note: this is a simple fallback. If you have access to a [JenkinsRule], use `getLog(Run)`.
 */
val Run<*, *>.log: String
    @Suppress("DEPRECATION") // Suppressing any potential deprecation warnings on log reading
    get() = this.logFile.readText()
