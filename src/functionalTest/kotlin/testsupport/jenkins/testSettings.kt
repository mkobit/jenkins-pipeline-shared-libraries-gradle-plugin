package testsupport.jenkins

const val WORKFLOW_API = "org.jenkins-ci.plugins.workflow:workflow-api"

fun jenkinsSettings(
  projectName: String = "test-project",
  includes: List<String> = emptyList(),
  includeBuild: String? = null,
): String {
  val base =
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
  return buildString {
    append(base)
    if (includes.isNotEmpty()) append("\ninclude(${includes.joinToString { "\"$it\"" }})")
    if (includeBuild != null) append("\nincludeBuild(\"$includeBuild\")")
  }
}
