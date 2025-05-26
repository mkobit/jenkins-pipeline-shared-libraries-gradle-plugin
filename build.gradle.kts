import buildsrc.ProjectInfo
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.jvm.tasks.Jar
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream
import java.net.URL

plugins {
  `kotlin-dsl`
  `java-library`

  id("org.jlleitschuh.gradle.ktlint") version "9.2.1"

  id("nebula.release") version "11.1.0"
  `maven-publish`
  id("com.gradle.plugin-publish") version "0.10.1"
  id("org.jetbrains.dokka") version "0.9.18"

  id("com.github.ben-manes.versions") version "0.28.0"

  // Only used for local publishing for testing
  buildsrc.`jenkins-rebaseline`
}

group = "com.mkobit.jenkins.pipelines"
description = "Gradle plugins for Jenkins Shared libraries usage"

val gitCommitSha: String by lazy {
  ByteArrayOutputStream().use {
    project.exec {
      commandLine("git", "rev-parse", "HEAD")
      standardOutput = it
    }
    it.toString(Charsets.UTF_8.name()).trim()
  }
}

val SourceSet.kotlin: SourceDirectorySet
  get() = withConvention(KotlinSourceSet::class) { kotlin }

fun env(key: String): String? = System.getenv(key)
buildScan {
  termsOfServiceAgree = "yes"
  termsOfServiceUrl = "https://gradle.com/terms-of-service"
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

sourceSets {
  // This source set used for resources to get IDE completion for ease of writing tests against JenkinsPipelineUnit and Jenkins Test Harness
  val pipelineTestResources by creating {
    java.setSrcDirs(emptyList<Any>())
    kotlin.setSrcDirs(emptyList<Any>())
    resources.setSrcDirs(listOf(file("src/$name")))
  }

  test {
    runtimeClasspath += pipelineTestResources.output
  }
}

repositories {
  mavenCentral()
  maven { url = uri("https://jitpack.io") }
  maven { url = uri("https://repo.jenkins-ci.org/public/") }
}

ktlint {
  version.set("0.37.2")
}

configurations {
  // This is very similar to the dependency resolution used in the plugin implementation.
  // It is mainly used to get IDEA autocompletion and for use it caching dependencies
  // These are used for code completion in the pipelineTestResources to more easily facilitate writing tests
  // against the libraries that are used.
  val pipelineTestResources by sourceSets.getting

  val resourcesCompileOnly = get(pipelineTestResources.compileOnlyConfigurationName)
  val jenkinsPlugins by creating {
    incoming.afterResolve {
      val resolvedArtifacts = resolvedConfiguration.resolvedArtifacts
      resolvedArtifacts
        .filter { it.extension in setOf("hpi", "jpi") }
        .map { "${it.moduleVersion}@jar" } // Use the published JAR libraries for each plugin
        .forEach { project.dependencies.add(resourcesCompileOnly.name, it) }
      // Include all of the additional JAR dependencies from the transitive dependencies of the plugin
      resolvedArtifacts
        .filter { it.extension == "jar" }
        .map { "${it.moduleVersion}@jar" } // TODO: might not need this
        .forEach { project.dependencies.add(resourcesCompileOnly.name, it) }
    }
  }
  resourcesCompileOnly.extendsFrom(jenkinsPlugins)
  all {
    incoming.beforeResolve {
      if (hierarchy.contains(resourcesCompileOnly)) {
        // Trigger the dependency seeding
        jenkinsPlugins.resolve()
      }
    }
    resolutionStrategy.eachDependency {
      when (requested.group) {
        "com.squareup.okhttp3" -> useVersion("3.14.1")
        "dev.minutest" -> useVersion("1.13.0")
        "org.junit.jupiter" -> useVersion("5.5.1")
        "org.junit.platform" -> useVersion("1.5.1")
        "io.strikt" -> useVersion("0.31.0")
        "org.apache.logging.log4j" -> useVersion("2.12.0")
        "com.christophsturm" -> useVersion("0.1.3")
      }
    }
  }
}

dependencies {
  api(gradleApi())
  api("com.squareup:javapoet:1.12.1")

  implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.4.32"))
  implementation("io.github.microutils:kotlin-logging:1.7.10")
  implementation("com.squareup.okhttp3:okhttp")

  testImplementation(kotlin("reflect"))
  testImplementation("com.github.mkobit:gradle-test-kotlin-extensions:0.7.0")
  testImplementation("io.mockk:mockk:1.10.0")
  testImplementation("com.google.guava:guava:29.0-jre")
  testImplementation("com.squareup.okhttp3:mockwebserver")

  testImplementation("io.strikt:strikt-core")
  testImplementation("io.strikt:strikt-jvm")
  testImplementation("io.strikt:strikt-gradle")

  testImplementation("dev.minutest:minutest")
  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testImplementation("org.junit.jupiter:junit-jupiter-params")

  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
  testRuntimeOnly("org.apache.logging.log4j:log4j-core")
  testRuntimeOnly("org.apache.logging.log4j:log4j-jul")

  // These are used for code completion in the pipelineTestResources to more easily facilitate writing tests
  // against the libraries that are used.
  val pipelineTestResources by sourceSets.getting
  pipelineTestResources.compileOnlyConfigurationName("com.lesfurets:jenkins-pipeline-unit:1.1")
  pipelineTestResources.compileOnlyConfigurationName("org.jenkins-ci.main:jenkins-test-harness:2.64")
  pipelineTestResources.compileOnlyConfigurationName("org.codehaus.groovy:groovy:2.4.12")
  val jenkinsPluginDependencies = listOf(
    "org.jenkins-ci.plugins.workflow:workflow-api:2.40",
    "org.jenkins-ci.plugins.workflow:workflow-basic-steps:2.20",
    "org.jenkins-ci.plugins.workflow:workflow-cps:2.80",
    "org.jenkins-ci.plugins.workflow:workflow-cps-global-lib:2.16",
    "org.jenkins-ci.plugins.workflow:workflow-durable-task-step:2.35",
    "org.jenkins-ci.plugins.workflow:workflow-job:2.39",
    "org.jenkins-ci.plugins.workflow:workflow-multibranch:2.21",
    "org.jenkins-ci.plugins.workflow:workflow-scm-step:2.11",
    "org.jenkins-ci.plugins.workflow:workflow-step-api:2.22",
    "org.jenkins-ci.plugins.workflow:workflow-support:3.5"
  )
  jenkinsPluginDependencies.forEach {
    "jenkinsPlugins"(it)
  }
  "jenkinsPlugins"("org.jenkins-ci.main:jenkins-core:2.222.4") {
    isTransitive = false
  }
}

tasks {
  wrapper {
    gradleVersion = "6.8.3"
  }

  dependencyUpdates {
    val rejectPatterns = listOf("alpha", "beta", "rc", "cr", "m").map { qualifier ->
      Regex("(?i).*[.-]$qualifier[.\\d-]*")
    }
    resolutionStrategy {
      componentSelection {
        all {
          if (rejectPatterns.any { it.matches(candidate.version) }) {
            reject("Release candidate")
          }
        }
      }
    }
  }

  withType<Jar>().configureEach {
    from(project.projectDir) {
      include("LICENSE.txt")
      into("META-INF")
    }
    manifest {
      attributes(
        mapOf(
          "Build-Revision" to gitCommitSha,
          "Implementation-Version" to project.version
        )
      )
    }
  }

  withType<Javadoc>().configureEach {
    options {
      header = project.name
      encoding = "UTF-8"
    }
  }

  withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
  }

  test {
    useJUnitPlatform()
    systemProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")
    project.findProperty("gradleTestVersions")?.let {
      // Will rerun some tests unfortunately using this method, but helps with CI
      systemProperty("testsupport.junit.ForGradleVersions.versions", it)
    }
    if (JavaVersion.current() > JavaVersion.VERSION_1_8) {
      jvmArgs("-XshowSettings:vm", "-Xlog:gc*", "-Xmx512m", "-Xms256m")
    } else {
      jvmArgs("-XshowSettings:vm", "-XX:+PrintGCTimeStamps", "-XX:+UseG1GC", "-Xmx512m", "-Xms256m")
    }
  }

  val sourcesJar by registering(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles a JAR of the source code"
    archiveClassifier.set("sources")
    from(sourceSets.main.map { it.allSource })
  }

  // No Java code, so don't need the javadoc task.
  // Dokka generates our documentation.
  javadoc {
    enabled = false
  }

  dokka {
    dependsOn(sourceSets.main.map { it.classesTaskName })
    jdkVersion = 8
    outputFormat = "html"
    outputDirectory = "$buildDir/javadoc"
    sourceDirs = sourceSets.main.get().kotlin.srcDirs
    externalDocumentationLink {
      url = URL("https://docs.gradle.org/${GradleVersion.current().version}/javadoc/")
    }
    externalDocumentationLink {
      url = URL("https://docs.oracle.com/javase/8/docs/api/")
    }
    externalDocumentationLink {
      url = URL("https://square.github.io/javapoet/javadoc/javapoet/")
    }
  }

  val javadocJar by registering(Jar::class) {
    description = "Assembles a JAR of the generated Javadoc"
    from(dokka.map { it.outputDirectory })
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    archiveClassifier.set("javadoc")
  }

  assemble {
    dependsOn(sourcesJar, javadocJar)
  }

  prepare {
    // disable Git upstream checks
    enabled = false
  }

  (release) {
    // disable git tag push
    enabled = false
    dependsOn(publishPlugins, build)
  }
}

artifacts {
  val sourcesJar by tasks.getting
  val javadocJar by tasks.getting
  add("archives", sourcesJar)
  add("archives", javadocJar)
}

gradlePlugin {
  plugins {
    // Don't get the extensions for NamedDomainObjectContainer here because we only have a NamedDomainObjectContainer
    // See https://github.com/gradle/kotlin-dsl/issues/459
    create("sharedLibrary") {
      id = "com.mkobit.jenkins.pipelines.shared-library"
      implementationClass = "com.mkobit.jenkins.pipelines.SharedLibraryPlugin"
      displayName = "Jenkins Pipeline Shared Library Development"
      description = "Configures and sets up a Gradle project for development and testing of a Jenkins Pipeline shared library (https://jenkins.io/doc/book/pipeline/shared-libraries/)"
    }
    create("jenkinsIntegration") {
      id = "com.mkobit.jenkins.pipelines.jenkins-integration"
      implementationClass = "com.mkobit.jenkins.pipelines.JenkinsIntegrationPlugin"
      displayName = "Jenkins Integration Plugin"
      description = "Tasks to retrieve information from a Jenkins instance to be aid in the development of tools with Gradle"
    }
  }
}

afterEvaluate {
  // https://github.com/gradle/gradle/issues/9565
  publishing {
    publications {
      getByName("pluginMaven", MavenPublication::class) {
        versionMapping {
          allVariants {
            fromResolutionOf("runtimeClasspath")
          }
        }
      }
    }
  }
}

pluginBundle {
  vcsUrl = ProjectInfo.projectUrl
  tags = listOf("jenkins", "pipeline", "shared library", "global library")
  website = ProjectInfo.projectUrl
}
