import org.gradle.util.GradleVersion

plugins {
  java
}

val testMatrix = extensions.create("testMatrix", TestMatrix::class.java)

tasks {
  val ciDir = layout.buildDirectory.dir("ci")

  register<GenerateJsonMatrix>("generateGradleCompatMatrix") {
    group = "CI"
    description = "Writes the Gradle compat CI matrix JSON to <build>/ci/gradle-compat-matrix.json"
    matrixEntries = testMatrix.gradleVersions.map { mapOf("gradle" to it) }
    outputFile = ciDir.map { it.file("gradle-compat-matrix.json") }
  }

  register<GenerateJsonMatrix>("generateJavaCompatMatrix") {
    group = "CI"
    description = "Writes the Java compat CI matrix JSON to <build>/ci/java-compat-matrix.json"
    matrixEntries = testMatrix.javaVersions.map { mapOf("java" to it.toString()) }
    outputFile = ciDir.map { it.file("java-compat-matrix.json") }
  }

  register<GenerateJenkinsCompatMatrix>("generateJenkinsCompatMatrix") {
    group = "CI"
    description = "Writes the Jenkins LTS compat CI matrix JSON to <build>/ci/jenkins-compat-matrix.json"
    for (entry in testMatrix.jenkinsLtsEntries) {
      entries.add(
        objects.newInstance<JenkinsMatrixEntry>().also {
          it.lts = entry.lts
          it.version = entry.version
          it.bomVersion = entry.bomVersion
        },
      )
    }
    outputFile = ciDir.map { it.file("jenkins-compat-matrix.json") }
  }

  register<GenerateBuildConfig>("generateBuildConfig") {
    group = "CI"
    description = "Writes the wrapper Gradle version and Java toolchain spec to <build>/ci/build-config.json"
    gradleVersion = GradleVersion.current().version
    javaVersion = project.java.toolchain.languageVersion.map { it.asInt() }
    outputFile = ciDir.map { it.file("build-config.json") }
  }
}
