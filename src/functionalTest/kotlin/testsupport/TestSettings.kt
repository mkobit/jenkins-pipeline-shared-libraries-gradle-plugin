package testsupport

fun jenkinsSettings(projectName: String = "test-project") =
  """
  dependencyResolutionManagement {
      repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
      repositories {
          mavenCentral()
          maven("https://repo.jenkins-ci.org/public/")
      }
  }
  rootProject.name = "$projectName"
  """.trimIndent()
