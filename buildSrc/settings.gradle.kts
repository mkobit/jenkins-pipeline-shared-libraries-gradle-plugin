@file:Suppress("UnstableApiUsage")

  pluginManagement {
    repositories {
      mavenLocal()
      mavenCentral()
      gradlePluginPortal()
    }
  }

  dependencyResolutionManagement {
    repositories {
      mavenLocal()
      mavenCentral()
      maven(url = "https://repo.jenkins-ci.org/public/")
    }

    versionCatalogs {
      create("libs") {
        from(files("../gradle/libs.versions.toml"))
      }
    }
  }

  rootProject.buildFileName = "buildSrc.gradle.kts"

  apply(from = file("../gradle/buildCache.settings.gradle.kts"))
