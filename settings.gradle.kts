pluginManagement {
  repositories {
    gradlePluginPortal()
  }
}

plugins {
  alias(libs.plugins.foojay)
}

dependencyResolutionManagement {
  repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
  repositories {
    mavenCentral()
    maven("https://repo.jenkins-ci.org/public/")
  }
}

rootProject.name = "jenkins-pipeline-shared-libraries-gradle-plugin"
