package testsupport.jenkins

const val WORKFLOW_API = "org.jenkins-ci.plugins.workflow:workflow-api"

fun jenkinsSettings(projectName: String = "test-project") =
  """
  plugins {
      id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
  }
  dependencyResolutionManagement {
      repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
      repositories {
          mavenCentral()
          maven("https://repo.jenkins-ci.org/public/")
      }
  }
  rootProject.name = "$projectName"
  """.trimIndent()
