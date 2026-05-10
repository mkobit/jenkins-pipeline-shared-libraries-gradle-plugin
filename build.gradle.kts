import org.gradle.util.GradleVersion

plugins {
  `kotlin-dsl`
  alias(libs.plugins.dokka)
  alias(libs.plugins.gradle.publish)
  alias(libs.plugins.openrewrite)
  alias(libs.plugins.spotless)
}

// Not included in the published plugin JAR — matrix registry and data types only.
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
        implementation(libs.kotest.decoroutinator)
        implementation(libs.kotest.runner)
      }
    }

    register<JvmTestSuite>("functionalTest") {
      useJUnitJupiter(libs.versions.junit.jupiter)
      dependencies {
        implementation(gradleTestKit())
        implementation(platform(libs.kotest.bom))
        implementation(libs.kotest.assertions)
        implementation(libs.kotest.decoroutinator)
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
            project.findProperty("kotest.tags") ?: "!resolution & !jenkins-compat",
          )
          // GradleRunner builds are I/O-bound and start no Jenkins instance, so
          // parallelism is safe. Split test classes across N forks and let Kotest
          // run N specs concurrently within each fork via coroutines.
          // jvmArgumentProviders (not systemProperty) so parallelism doesn't pollute the cache key.
          val cpuHalf = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
          maxParallelForks = cpuHalf
          jvmArgumentProviders += CommandLineArgumentProvider { listOf("-Dkotest.framework.parallelism=$cpuHalf") }
          // Pin to a single Gradle version for fast debugging: -Ptest.gradle.version=9.5.0
          // Use -Ptest.gradle.version=current to target the wrapper version automatically.
          project.findProperty("test.gradle.version")?.let { prop ->
            val version = if (prop == "current") GradleVersion.current().version else prop.toString()
            systemProperty("test.gradle.version", version)
          }
          project.findProperty("test.jenkins.version")?.let { systemProperty("test.jenkins.version", it.toString()) }
        }
      }
    }
  }
}

dependencies {
  "functionalTestImplementation"(ciMatrixSourceSet.output)
}

val ftSuite = testing.suites.getByName<JvmTestSuite>("functionalTest")

// Stable task for CI jobs that test Java or platform variation (not Gradle version variation).
// Pins to the current wrapper version; gradle-compat CI uses -Ptest.gradle.version=X directly.
tasks.register<Test>("functionalTestCurrentWrapper") {
  group = "verification"
  description = "Functional tests for the current Gradle wrapper version (${GradleVersion.current().version})"
  testClassesDirs = ftSuite.sources.output.classesDirs
  classpath = ftSuite.sources.runtimeClasspath + files(tasks.pluginUnderTestMetadata)
  useJUnitPlatform()
  mustRunAfter(tasks.named("test"))
  systemProperty("kotest.filter.tags", project.findProperty("kotest.tags") ?: "!resolution & !jenkins-compat")
  systemProperty("test.gradle.version", GradleVersion.current().version)
  maxParallelForks = 1
  jvmArgumentProviders += CommandLineArgumentProvider { listOf("-Dkotest.framework.parallelism=3") }
  reports {
    html.outputLocation.set(layout.buildDirectory.dir("reports/tests/functionalTestCurrentWrapper"))
    junitXml.outputLocation.set(layout.buildDirectory.dir("test-results/functionalTestCurrentWrapper"))
  }
}

tasks.check {
  dependsOn("functionalTestCurrentWrapper")
}

tasks.withType<Test>().configureEach {
  // Share a TestKit working directory across GradleRunner invocations so Jenkins artifact
  // downloads are cached between test cases in the same job. Controlled by GRADLE_USER_HOME
  // (set by gradle/actions/setup-gradle in CI); absent locally, each runner uses a fresh temp dir.
  System.getenv("GRADLE_USER_HOME")?.let { systemProperty("test.gradle.user.home", it) }
  testLogging {
    events("failed", "skipped")
    showExceptions = true
    showCauses = true
    showStackTraces = true
    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    stackTraceFilters = setOf(org.gradle.api.tasks.testing.logging.TestStackTraceFilter.TRUNCATE)
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
