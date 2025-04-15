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

repositories {
  mavenCentral()
}

ktlint {
  version.set("0.32.0")
}

dependencies {
  implementation("com.fasterxml.jackson.core:jackson-core:2.9.9")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.9.9")
  implementation("com.squareup.retrofit2:converter-jackson:2.5.0")
  implementation("com.squareup.retrofit2:retrofit:2.5.0")
}

tasks {
  dependencyUpdates {
    val rejectPatterns = listOf("alpha", "beta", "rc", "cr", "m").map { qualifier ->
      Regex("(?i).*[.-]$qualifier[.\\d-]*")
    }
    resolutionStrategy {
      componentSelection {
        all {
          if (rejectPatterns.any { it.matches(candidate.version) }) {
            reject("Release candidate")
          }
        }
      }
    }
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
