pluginManagement {
  includeBuild("../..") // the plugin itself
  repositories {
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
  repositories {
    mavenCentral()
    maven("https://repo.jenkins-ci.org/public/")
  }
}

rootProject.name = "peer-libraries-composite"

// library-a internally includes included-lib-3; Gradle composes nested included builds,
// so included-lib-3 is available for GAV substitution without an explicit includeBuild here.
includeBuild("library-a")
includeBuild("library-b")
