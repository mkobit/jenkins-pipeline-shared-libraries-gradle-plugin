import org.gradle.api.tasks.Input

data class JenkinsLtsEntry(
  @get:Input val lts: String,
  @get:Input val version: String,
  @get:Input val bomVersion: String,
)
