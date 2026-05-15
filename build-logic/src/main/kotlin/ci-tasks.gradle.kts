import org.gradle.util.GradleVersion

plugins {
  java
}

val gradleCompatVersions =
  (listOf("9.0.0", "9.1.0", "9.2.1", "9.3.1", "9.4.1", "9.5.0")
    .map { GradleVersion.version(it) } + GradleVersion.current())
    .toSortedSet()
    .map { it.version }

val javaCompatVersions = listOf(21, 25)

// lts label, version, BOM version — drives both the CI matrix JSON and TestedJenkinsVersion.all.
val jenkinsLtsEntries: List<Triple<String, String, String>> =
  listOf(
    Triple("2.479.x", "2.479.1", "5054.v620b_5d2b_d5e6"),
    Triple("2.528.x", "2.528.3", "6398.v1d26a_dd495e2"),
    Triple("2.541.x", "2.541.3", "6364.v16b_76a_4023c7"),
  )

// Inject version lists into every Test task so test-support classes can read them
// without hardcoding anything in functional-test source.
tasks.withType<Test>().configureEach {
  systemProperty("test.gradle.versions", gradleCompatVersions.joinToString(","))
  systemProperty(
    "test.jenkins.entries",
    jenkinsLtsEntries.joinToString(",") { (lts, v, bom) -> "$lts|$v|$bom" },
  )
}

tasks {
  val ciDir = layout.buildDirectory.dir("ci")

  register<GenerateJsonMatrix>("generateGradleCompatMatrix") {
    group = "CI"
    description = "Writes the Gradle compat CI matrix JSON to <build>/ci/gradle-compat-matrix.json"
    matrixEntries = gradleCompatVersions.map { mapOf("gradle" to it) }
    outputFile = ciDir.map { it.file("gradle-compat-matrix.json") }
  }

  register<GenerateJsonMatrix>("generateJavaCompatMatrix") {
    group = "CI"
    description = "Writes the Java compat CI matrix JSON to <build>/ci/java-compat-matrix.json"
    matrixEntries = javaCompatVersions.map { mapOf("java" to it.toString()) }
    outputFile = ciDir.map { it.file("java-compat-matrix.json") }
  }

  register<GenerateJenkinsCompatMatrix>("generateJenkinsCompatMatrix") {
    group = "CI"
    description = "Writes the Jenkins LTS compat CI matrix JSON to <build>/ci/jenkins-compat-matrix.json"
    for ((lts, version, bom) in jenkinsLtsEntries) {
      entries.add(
        objects.newInstance<JenkinsMatrixEntry>().also {
          it.lts = lts
          it.version = version
          it.bomVersion = bom
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
