import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestStackTraceFilter

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
    val testSuite = named<JvmTestSuite>("test") {
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
        implementation(project())
        implementation(gradleTestKit())
        implementation(platform(libs.kotest.bom))
        implementation(libs.kotest.assertions)
        implementation(libs.kotest.decoroutinator)
        implementation(libs.kotest.engine)
        runtimeOnly(libs.kotest.runner)
        // Puts plugin-under-test-metadata.properties on the runtime classpath so
        // GradleRunner.withPluginClasspath() can find the plugin under test.
        runtimeOnly(files(tasks.pluginUnderTestMetadata.flatMap { it.outputDirectory }))
      }

      targets.configureEach {
        testTask.configure {
          mustRunAfter(testSuite)
        }
      }

      // One target per matrix variant (gate + CI fan-out).
      // Each variant declares which axes are pinned; null means "not applicable".
      matrix.allVariants.map { variant ->
        targets.register(variant.taskName) {
          testTask.configure {
            variant.javaVersion?.let {
              javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(it) }
            }
            val effectiveTagFilter = project.findProperty("kotest.tags")?.toString() ?: variant.tagFilter
            effectiveTagFilter?.let { systemProperty("kotest.filter.tags", it) }
            variant.gradleVersion?.let { systemProperty("test.gradle.version", it) }
            variant.jenkinsEntries?.let { entries ->
              systemProperty(
                "test.jenkins.entries",
                entries.joinToString(",") { "${it.lts}|${it.version}|${it.bomVersion}" },
              )
            }
            maxParallelForks = 1
            jvmArgumentProviders += CommandLineArgumentProvider { listOf("-Dkotest.framework.parallelism=3") }
          }
        }
      }
    }
  }
}

// Gate: build Java + current Gradle wrapper + all Jenkins LTS, no tag filter.
tasks.register("functionalTestCurrent") {
  group = JavaBasePlugin.VERIFICATION_GROUP
  description = "Gate: build Java × current Gradle wrapper × all Jenkins LTS entries"
  dependsOn(matrix.currentVariant.taskName)
}

tasks.check {
  dependsOn("functionalTestCurrent")
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
