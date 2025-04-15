import org.jlleitschuh.gradle.ktlint.KtlintFormatTask

plugins {
  `kotlin-dsl`
  alias(libs.plugins.ktlint)
  alias(libs.plugins.benManesVersions)
}

tasks.withType<KotlinCompile>().configureEach {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_11)
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }
}

ktlint {
  coloredOutput.set(false)
  ignoreFailures.set(true)
}

dependencies {
  implementation(platform(libs.jackson.bom))
  implementation(libs.bundles.jackson)

  implementation(platform(libs.retrofit.bom))
  implementation(libs.bundles.retrofit)
}

tasks {
  dependencyUpdates {
    rejectVersionIf {
      // Don't reject stable releases when the current version is unstable
      if (isNonStable(currentVersion) && !isNonStable(candidate.version)) {
        return@rejectVersionIf false
      }

      // Reject unstable versions
      isNonStable(candidate.version)
    }

    // Optional: specify an output format
    outputFormatter = "html,json"
    outputDir = "build/reports/dependency-updates"
    reportfileName = "dependencies"

    // Optional: check for Gradle updates
    checkForGradleUpdate = true

    // Optional: Stability for a Gradle update channel
    gradleReleaseChannel = "current"
  }

  assemble {
    dependsOn(withType<org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask>())
  }

  withType<org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask>().configureEach {
    onlyIf {
      project.hasProperty("ktlintFormatBuildSrc")
    }
  }

  dependencyUpdates {
    onlyIf { project.hasProperty("updateBuildSrc") }
  }

  build {
    dependsOn(dependencyUpdates)
  }
}

gradlePlugin {
  plugins.invoke {
    // Don't get the extensions for NamedDomainObjectContainer here because we only have a NamedDomainObjectContainer
    // See https://github.com/gradle/kotlin-dsl/issues/459
    register("sharedLibrary") {
      id = "buildsrc.jenkins-rebaseline"
      implementationClass = "buildsrc.jenkins.baseline.JenkinsRebaselineToolsPlugin"
    }
  }
}

/**
 * Determines if a version string represents a non-stable (preview) version.
 * This improved function checks for common unstable version patterns:
 * - Contains alpha, beta, rc, cr, m, dev, snapshot (case-insensitive)
 * - Contains "SNAPSHOT" suffix
 * - Contains number followed by non-digit non-dot (e.g. 1.2.0-M1)
 * - Doesn't contain RELEASE, FINAL, GA (case-insensitive)
 */
fun isNonStable(version: String): Boolean {
  val stableKeywords = listOf("RELEASE", "FINAL", "GA")
  val stableKeywordRegex = stableKeywords.joinToString("|") { "(?i).*$it.*" }.toRegex()

  val unstableKeywords = listOf("alpha", "beta", "rc", "cr", "m", "dev", "preview", "eap", "snapshot")
  val unstableKeywordRegex = unstableKeywords.joinToString("|") { "(?i).*[.-]$it[.\\d-]*" }.toRegex()

  if (stableKeywordRegex.matches(version)) {
    return false
  }

  return unstableKeywordRegex.matches(version)
    || "(?i).*-SNAPSHOT.*".toRegex().matches(version)
    || "^[0-9,.v-]+(-r)?$".toRegex().matches(version).not()
}
