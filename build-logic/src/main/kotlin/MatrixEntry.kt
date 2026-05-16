import org.gradle.api.tasks.Input

data class MatrixEntry(
  @get:Input val fields: Map<String, String>,
)
