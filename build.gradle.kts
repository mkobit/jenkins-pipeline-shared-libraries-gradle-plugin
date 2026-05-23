import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestStackTraceFilter
import org.gradle.kotlin.dsl.testMatrix
import org.gradle.plugin.compatibility.compatibility
import org.jetbrains.dokka.gradle.tasks.DokkaGeneratePublicationTask

plugins {
  `kotlin-dsl`
  alias(libs.plugins.dokka)
  alias(libs.plugins.gradle.publish)
  alias(libs.plugins.openrewrite)
  alias(libs.plugins.spotless)
  id("ci-tasks")
}

group = "com.mkobit.jenkins.pipelines"
apply(from = "gradle/version.gradle.kts")
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
      compatibility {
        features {
          configurationCache = true
        }
      }
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
    val testSuite =
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

      val kotestParallelism =
        (findProperty("kotest.parallelism") as? String)?.toInt()
          ?: Runtime.getRuntime().availableProcessors()

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
          jvmArgumentProviders += CommandLineArgumentProvider { listOf("-Dkotest.framework.parallelism=$kotestParallelism") }
        }
      }

      // CI fan-out: one target per matrix variant; each axis is pinned via nullable fields.
      val variants =
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
                jvmArgumentProviders += CommandLineArgumentProvider { listOf("-Dkotest.framework.parallelism=$kotestParallelism") }
              }
            }
        }

      tasks.register("functionalTestAll") {
        group = JavaBasePlugin.VERIFICATION_GROUP
        description = "Runs the gate and all CI fan-out matrix variants"
        dependsOn(gate, variants)
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

tasks.register<Exec>("example-basic-vars") {
  group = JavaBasePlugin.VERIFICATION_GROUP
  description = "Runs the basic-vars example build"
  workingDir = file("examples/basic-vars")
  commandLine(file("gradlew").absolutePath, "check")
}

// Restrict Dokka to only the hand-written src/main/kotlin sources. Without this, Dokka also
// picks up build/generated-sources/kotlin-dsl-plugins/kotlin/SharedLibraryPlugin.kt — the
// adapter Gradle generates for the precompiled script plugin — which has a KDoc @see reference
// to Shared_library_gradle that Dokka cannot resolve (the class is compiled from a .gradle.kts
// file and has no corresponding .kt source for Dokka to index).
dokka {
  dokkaSourceSets.main {
    sourceRoots.setFrom(layout.projectDirectory.dir("src/main/kotlin"))
  }
}

// gradle-plugin-publish creates javadocJar in its own afterEvaluate; configure it in ours
// (which runs after the plugin's) to replace the empty standard-javadoc output with Dokka HTML.
afterEvaluate {
  tasks.named("javadoc") { enabled = false }
  tasks.named<Jar>("javadocJar") {
    val dokkaHtml = tasks.named<DokkaGeneratePublicationTask>("dokkaGeneratePublicationHtml")
    from(dokkaHtml.flatMap { it.outputDirectory })
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
  toml {
    versionCatalog()
    target("gradle/libs.versions.toml")
  }
}
