import org.gradle.util.GradleVersion

plugins {
  `kotlin-dsl`
  alias(libs.plugins.dokka)
  alias(libs.plugins.gradle.publish)
  alias(libs.plugins.openrewrite)
  alias(libs.plugins.spotless)
}

// ── ciMatrix source set ────────────────────────────────────────────────────────
// Non-main, unbundled: not included in the published plugin JAR. Contains the
// matrix registry (MatrixCli.kt) and data types (CiMatrix.kt). Compiled against
// the main source set so it can reference SharedLibraryDefaults if needed.
// functionalTest can also depend on it for version cross-checking.
val ciMatrixSourceSet =
  sourceSets.create("ciMatrix") {
    kotlin.srcDir("src/ciMatrix/kotlin")
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath + output
  }

group = "com.mkobit.jenkins.pipelines"
version = "0.11.0"
description = "Gradle plugins for Jenkins Pipeline shared library development and testing"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(17)
  }
}

gradlePlugin {
  website = "https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin"
  vcsUrl = "https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin"
  plugins {
    named("com.mkobit.jenkins.pipelines.shared-library") {
      displayName = "Jenkins Pipeline Shared Library Development"
      description = "Configures and sets up a Gradle project for development and testing of a Jenkins Pipeline shared library"
      tags = listOf("jenkins", "pipeline", "shared library")
    }
  }
}

dependencies {
  api(gradleApi())
}

testing {
  suites {
    named<JvmTestSuite>("test") {
      useJUnitJupiter(libs.versions.junit.jupiter)
      dependencies {
        implementation(platform(libs.kotest.bom))
        implementation(libs.mockk)
        implementation(libs.kotest.assertions)
        implementation(libs.kotest.runner)
      }
    }

    register<JvmTestSuite>("functionalTest") {
      useJUnitJupiter(libs.versions.junit.jupiter)
      dependencies {
        implementation(gradleTestKit())
        implementation(platform(libs.kotest.bom))
        implementation(libs.kotest.assertions)
        implementation(libs.kotest.runner)
        implementation(project())
      }
      targets.configureEach {
        testTask.configure {
          mustRunAfter(tasks.named("test"))
          // java-gradle-plugin only wires pluginUnderTestMetadata into the test suite;
          // add it explicitly so GradleRunner.withPluginClasspath() works here.
          classpath += files(tasks.pluginUnderTestMetadata)
          // Resolution tests hit the Jenkins Maven repo and are slow on a cold cache.
          // Default to excluding them from the normal check so CI can run them as a
          // separate cacheable step. Override with -Pkotest.tags=resolution (or any
          // other Kotest tag expression) to target a specific subset.
          systemProperty(
            "kotest.filter.tags",
            project.findProperty("kotest.tags") ?: "!resolution",
          )
          // GradleRunner builds are I/O-bound and start no Jenkins instance, so
          // parallelism is safe. Split test classes across N forks and let Kotest
          // run N specs concurrently within each fork via coroutines.
          val cpuHalf = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
          maxParallelForks = cpuHalf
          systemProperty("kotest.framework.parallelism", cpuHalf)
          // Pin to a single Gradle version for fast debugging: -Ptest.gradle.version=9.5.0
          project.findProperty("test.gradle.version")?.let {
            systemProperty("test.gradle.version", it)
          }
        }
      }
    }
  }
}

// ── CI matrix data ────────────────────────────────────────────────────────────
// The canonical matrix registry lives in src/ciMatrix/kotlin/…/MatrixCli.kt.
// gradleCompatVersions is duplicated here because the per-version task fan-out
// below must run at configuration time (before the ciMatrix source set is compiled).
// Keep these two lists in sync.
val gradleCompatVersions = listOf("9.0.0", "9.1.0", "9.2.1", "9.3.1", "9.4.1")

dependencies {
  // Wire ciMatrix output into functionalTest so tests can reference the registry
  // (gradleCompatVersions, jenkinsGateEntry, etc.) without duplicating values.
  "functionalTestImplementation"(ciMatrixSourceSet.output)
}

// ── Per-version functional test tasks ─────────────────────────────────────────
// org.gradle.parallel=true (gradle.properties) runs them concurrently. Each
// reuses the compiled functionalTest source set and pins one version so
// withData(TestedGradleVersion.filtered) exercises only that version.
// The legacy `functionalTest` task remains for local debugging (-Ptest.gradle.version=X).
val ftSuite = testing.suites.getByName<JvmTestSuite>("functionalTest")
val perVersionTests =
  gradleCompatVersions
    .map { GradleVersion.version(it) }
    .plus(GradleVersion.current())
    .distinct()
    .map { gv ->
      val suffix = gv.version.replace(".", "_")
      tasks.register<Test>("functionalTest$suffix") {
        group = "verification"
        description = "Functional tests for Gradle ${gv.version}"
        testClassesDirs = ftSuite.sources.output.classesDirs
        classpath = ftSuite.sources.runtimeClasspath + files(tasks.pluginUnderTestMetadata)
        useJUnitPlatform()
        mustRunAfter(tasks.named("test"))
        systemProperty("kotest.filter.tags", project.findProperty("kotest.tags") ?: "!resolution")
        systemProperty("test.gradle.version", gv.version)
        maxParallelForks = 1
        systemProperty("kotest.framework.parallelism", 2)
        reports {
          html.outputLocation.set(layout.buildDirectory.dir("reports/tests/functionalTest$suffix"))
          junitXml.outputLocation.set(layout.buildDirectory.dir("test-results/functionalTest$suffix"))
        }
      }
    }

tasks.check {
  dependsOn(perVersionTests)
}

tasks.withType<Test>().configureEach {
  testLogging {
    events("failed", "skipped")
    showExceptions = true
    showCauses = true
    showStackTraces = true
    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
  }
}

val dokkaHtmlJar =
  tasks.register<Jar>("dokkaHtmlJar") {
    description = "Assembles Dokka HTML documentation into a JAR"
    archiveClassifier.set("javadoc")
    val dokkaHtml =
      tasks.named<org.jetbrains.dokka.gradle.tasks.DokkaGeneratePublicationTask>(
        "dokkaGeneratePublicationHtml",
      )
    dependsOn(dokkaHtml)
    from(dokkaHtml.flatMap { it.outputDirectory })
  }

// gradle-publish creates pluginMaven lazily; wire the javadoc artifact after evaluation.
afterEvaluate {
  publishing {
    publications {
      named<MavenPublication>("pluginMaven") {
        artifact(dokkaHtmlJar)
      }
    }
  }
}

tasks.wrapper {
  gradleVersion = "9.5.0"
  distributionType = Wrapper.DistributionType.ALL
}

// ── CI matrix generators ───────────────────────────────────────────────────────
// GitHub Actions reads the generated JSON files via `cat` and passes them to
// `fromJSON` for dynamic matrix strategy. MatrixCli (src/ciMatrix) is the
// canonical source of truth; these tasks invoke it via JavaExec.

tasks.register<JavaExec>("generateGradleCompatMatrix") {
  group = "CI"
  description = "Writes the gradle-compat CI matrix JSON to build/ci/gradle-compat-matrix.json"
  mainClass = "com.mkobit.jenkins.pipelines.ci.MatrixCliKt"
  classpath = ciMatrixSourceSet.runtimeClasspath
  val outFile = layout.buildDirectory.file("ci/gradle-compat-matrix.json")
  outputs.file(outFile)
  argumentProviders +=
    CommandLineArgumentProvider {
      listOf("gradle", outFile.get().asFile.absolutePath)
    }
}

tasks.register<JavaExec>("generateJenkinsCompatMatrix") {
  group = "CI"
  description = "Writes the jenkins-compat CI matrix JSON to build/ci/jenkins-compat-matrix.json"
  mainClass = "com.mkobit.jenkins.pipelines.ci.MatrixCliKt"
  classpath = ciMatrixSourceSet.runtimeClasspath
  val outFile = layout.buildDirectory.file("ci/jenkins-compat-matrix.json")
  outputs.file(outFile)
  argumentProviders +=
    CommandLineArgumentProvider {
      listOf("jenkins", outFile.get().asFile.absolutePath)
    }
}

tasks.register<JavaExec>("generateJenkinsGateParams") {
  group = "CI"
  description = "Writes the jenkins gate parameters JSON to build/ci/jenkins-gate-params.json"
  mainClass = "com.mkobit.jenkins.pipelines.ci.MatrixCliKt"
  classpath = ciMatrixSourceSet.runtimeClasspath
  val outFile = layout.buildDirectory.file("ci/jenkins-gate-params.json")
  outputs.file(outFile)
  argumentProviders +=
    CommandLineArgumentProvider {
      listOf("jenkins-gate", outFile.get().asFile.absolutePath)
    }
}

tasks.register<JavaExec>("generateJavaCompatMatrix") {
  group = "CI"
  description = "Writes the java-compat CI matrix JSON to build/ci/java-compat-matrix.json"
  mainClass = "com.mkobit.jenkins.pipelines.ci.MatrixCliKt"
  classpath = ciMatrixSourceSet.runtimeClasspath
  val outFile = layout.buildDirectory.file("ci/java-compat-matrix.json")
  outputs.file(outFile)
  argumentProviders +=
    CommandLineArgumentProvider {
      listOf("java-compat", outFile.get().asFile.absolutePath)
    }
}

spotless {
  kotlin {
    ktlint()
    target("src/**/*.kt")
    targetExclude("src/integrationTest/**/*.kt")
  }
  kotlinGradle {
    ktlint()
    target("*.gradle.kts", "src/**/*.gradle.kts")
  }
}
