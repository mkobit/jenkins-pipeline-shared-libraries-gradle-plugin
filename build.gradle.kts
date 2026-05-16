import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestStackTraceFilter
import org.gradle.kotlin.dsl.testMatrix

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

kotlin {
  compilerOptions {
    freeCompilerArgs.add("-Xcontext-parameters")
  }
}

dependencies {
  api(gradleApi())
}

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

      // Gate: default suite target — toolchain Java × current Gradle × all Jenkins LTS × no tag filter.
      val gate = targets.named("functionalTest")
      gate.configure {
        testTask {
          javaLauncher = javaToolchains.launcherFor { languageVersion = java.toolchain.languageVersion }
          systemProperty("test.gradle.version", testMatrix.current)
          systemProperty(
            "test.jenkins.entries",
            testMatrix.jenkinsLtsEntries.joinToString(",") { "${it.lts}|${it.version}|${it.bomVersion}" },
          )
          maxParallelForks = 1
          jvmArgumentProviders += CommandLineArgumentProvider { listOf("-Dkotest.framework.parallelism=3") }
        }
      }

      // CI fan-out: one target per matrix variant; each axis is pinned via nullable fields.
      val variantTasks =
        testMatrix.variants.map { variant ->
          targets
            .register(variant.taskName) {
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
            }.flatMap { it.testTask }
        }

      tasks.register("functionalTestAll") {
        group = JavaBasePlugin.VERIFICATION_GROUP
        description = "Runs the gate and all CI fan-out matrix variants"
        dependsOn(gate, variantTasks)
      }

      tasks.check {
        dependsOn(gate)
      }
    }
  }
}

tasks.withType<Test>().configureEach {
  // Share a TestKit working directory across GradleRunner invocations so Jenkins artifact
  // downloads are cached between test cases in the same job.
  System.getenv("GRADLE_USER_HOME")?.let { systemProperty("test.gradle.user.home", it) }
  testLogging {
    events("failed", "skipped")
    showStackTraces = true
    exceptionFormat = TestExceptionFormat.SHORT
    stackTraceFilters = setOf(TestStackTraceFilter.TRUNCATE)
  }
}

val dokkaHtmlJar =
  tasks.register<Jar>("dokkaHtmlJar") {
    description = "Assembles Dokka HTML documentation into a JAR"
    archiveClassifier = "javadoc"
    val dokkaHtml =
      tasks.named<org.jetbrains.dokka.gradle.tasks.DokkaGeneratePublicationTask>(
        "dokkaGeneratePublicationHtml",
      )
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
