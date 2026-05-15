import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestStackTraceFilter
import org.gradle.util.GradleVersion

plugins {
  `kotlin-dsl`
  alias(libs.plugins.dokka)
  alias(libs.plugins.gradle.publish)
  alias(libs.plugins.openrewrite)
  alias(libs.plugins.spotless)
  id("ci-tasks")
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

val matrix = testMatrix

testing {
  suites {
    named<JvmTestSuite>("test") {
      useJUnitJupiter(libs.versions.junit.jupiter)
      dependencies {
        implementation(platform(libs.kotest.bom))
        implementation(libs.mockk)
        implementation(libs.kotest.assertions)
        implementation(libs.kotest.decoroutinator)
        implementation(libs.kotest.engine)
        runtimeOnly(libs.kotest.runner)
      }
    }

    register<JvmTestSuite>("functionalTest") {
      useJUnitJupiter(libs.versions.junit.jupiter)
      dependencies {
        implementation(gradleTestKit())
        implementation(platform(libs.kotest.bom))
        implementation(libs.kotest.assertions)
        implementation(libs.kotest.decoroutinator)
        implementation(libs.kotest.engine)
        runtimeOnly(libs.kotest.runner)
        implementation(project())
      }

      // Applied to every target: plugin classpath, ordering, and version lists.
      targets.configureEach {
        testTask.configure {
          mustRunAfter(tasks.named("test"))
          // java-gradle-plugin only wires pluginUnderTestMetadata into the test suite;
          // add it explicitly so GradleRunner.withPluginClasspath() works here.
          classpath += files(tasks.pluginUnderTestMetadata)
          // Inject full version lists so TestedGradleVersion and TestedJenkinsVersion
          // can read them. Scoped here (not tasks.withType) to avoid polluting unit tests.
          systemProperty("test.gradle.versions", matrix.gradleVersions.joinToString(","))
          systemProperty("test.jenkins.entries", matrix.jenkinsLtsEntries.joinToString(",") { "${it.lts}|${it.version}|${it.bomVersion}" })
        }
      }

      // Default target: all-versions run, parallel forks, property overrides.
      targets.named("functionalTest") {
        testTask.configure {
          systemProperty("kotest.filter.tags", project.findProperty("kotest.tags") ?: "!Resolution & !JenkinsCompat")
          // GradleRunner builds are I/O-bound, so parallelism is safe.
          val cpuHalf = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
          maxParallelForks = cpuHalf
          jvmArgumentProviders += CommandLineArgumentProvider { listOf("-Dkotest.framework.parallelism=$cpuHalf") }
          project.findProperty("test.gradle.version")?.let { prop ->
            val version = if (prop == "current") GradleVersion.current().version else prop.toString()
            systemProperty("test.gradle.version", version)
          }
          project.findProperty("test.jenkins.version")?.let { systemProperty("test.jenkins.version", it.toString()) }
        }
      }

      // Pins to the current wrapper; used by `check` and Java/platform compat CI.
      targets.register("functionalTestCurrentWrapper") {
        testTask.configure {
          systemProperty("kotest.filter.tags", project.findProperty("kotest.tags") ?: "!Resolution & !JenkinsCompat")
          systemProperty("test.gradle.version", GradleVersion.current().version)
          maxParallelForks = 1
          jvmArgumentProviders += CommandLineArgumentProvider { listOf("-Dkotest.framework.parallelism=3") }
          reports {
            html.outputLocation.set(layout.buildDirectory.dir("reports/tests/functionalTestCurrentWrapper"))
            junitXml.outputLocation.set(layout.buildDirectory.dir("test-results/functionalTestCurrentWrapper"))
          }
        }
      }

      // One target per Gradle compat version for IDE visibility and targeted debugging.
      matrix.gradleVersions.forEach { version ->
        val suffix = "Gradle${version.replace(".", "_")}"
        targets.register("functionalTest$suffix") {
          testTask.configure {
            systemProperty("kotest.filter.tags", project.findProperty("kotest.tags") ?: "!Resolution & !JenkinsCompat")
            systemProperty("test.gradle.version", version)
            maxParallelForks = 1
            jvmArgumentProviders += CommandLineArgumentProvider { listOf("-Dkotest.framework.parallelism=3") }
            reports {
              html.outputLocation.set(layout.buildDirectory.dir("reports/tests/functionalTest$suffix"))
              junitXml.outputLocation.set(layout.buildDirectory.dir("test-results/functionalTest$suffix"))
            }
          }
        }
      }

      // One target per Jenkins LTS for IDE visibility; defaults to JenkinsCompat tag filter.
      matrix.jenkinsLtsEntries.forEach { entry ->
        val suffix = "Jenkins${entry.lts.replace(".", "").replace("x", "")}"
        targets.register("functionalTest$suffix") {
          testTask.configure {
            systemProperty("kotest.filter.tags", project.findProperty("kotest.tags") ?: "JenkinsCompat")
            systemProperty("test.jenkins.version", entry.version)
            maxParallelForks = 1
            jvmArgumentProviders += CommandLineArgumentProvider { listOf("-Dkotest.framework.parallelism=3") }
            reports {
              html.outputLocation.set(layout.buildDirectory.dir("reports/tests/functionalTest$suffix"))
              junitXml.outputLocation.set(layout.buildDirectory.dir("test-results/functionalTest$suffix"))
            }
          }
        }
      }
    }
  }
}

tasks.check {
  dependsOn("functionalTestCurrentWrapper")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  compilerOptions.freeCompilerArgs.add("-Xcontext-parameters")
}

tasks.withType<Test>().configureEach {
  // Share a TestKit working directory across GradleRunner invocations so Jenkins artifact
  // downloads are cached between test cases in the same job.
  System.getenv("GRADLE_USER_HOME")?.let { systemProperty("test.gradle.user.home", it) }
  testLogging {
    events("failed", "skipped")
    showStackTraces = true
    exceptionFormat = TestExceptionFormat.FULL
    stackTraceFilters = setOf(TestStackTraceFilter.TRUNCATE)
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
  gradleVersion = "9.4.1"
  distributionType = Wrapper.DistributionType.ALL
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
