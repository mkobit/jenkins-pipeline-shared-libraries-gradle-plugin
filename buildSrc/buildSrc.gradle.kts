plugins {
  `kotlin-dsl`
  id("com.github.ben-manes.versions") version "0.21.0"
}

repositories {
  jcenter()
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
