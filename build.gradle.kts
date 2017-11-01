import com.gradle.publish.PluginConfig
import com.gradle.publish.PublishPlugin
import org.gradle.api.internal.HasConvention
import org.gradle.jvm.tasks.Jar
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.preprocessor.mkdirsOrFail
import org.junit.platform.console.options.Details
import org.junit.platform.gradle.plugin.JUnitPlatformExtension
import java.io.ByteArrayOutputStream

buildscript {
  val dokkaVersion = "0.9.15"
  repositories {
    mavenCentral()
    jcenter()
  }
  dependencies {
    // TODO: load from properties or script plugin
    classpath("org.junit.platform:junit-platform-gradle-plugin:1.0.1")
    classpath("org.jetbrains.dokka:dokka-gradle-plugin:${dokkaVersion}")
  }
}

plugins {
  id("com.gradle.build-scan") version "1.10.1"
  `kotlin-dsl`
  `java-library`
  `java-gradle-plugin`
  id("com.gradle.plugin-publish") version "0.9.9"
  id("com.github.ben-manes.versions") version "0.17.0"
  // Only used for local publishing for testing
  `maven-publish`
}

apply {
  plugin("org.junit.platform.gradle.plugin")
  plugin("org.jetbrains.dokka")
  from("gradle/junit5.gradle.kts")
}

version = "0.3.0"
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

buildScan {
  fun env(key: String): String? = System.getenv(key)

  setLicenseAgree("yes")
  setLicenseAgreementUrl("https://gradle.com/terms-of-service")

  // Env variables from https://circleci.com/docs/2.0/env-vars/
  if (env("CI") != null) {
    logger.lifecycle("Running in CI environment, setting build scan attributes.")
    tag("CI")
    env("CIRCLE_BRANCH")?.let { tag(it) }
    env("CIRCLE_BUILD_NUM")?.let { value("Circle CI Build Number", it) }
    env("CIRCLE_BUILD_URL")?.let { link("Build URL", it) }
    env("CIRCLE_SHA1")?.let { value("Revision", it) }
    env("CIRCLE_COMPARE_URL")?.let { link("Diff", it) }
    env("CIRCLE_REPOSITORY_URL")?.let { value("Repository", it) }
    env("CIRCLE_PR_NUMBER")?.let { value("Pull Request Number", it) }
    link("Repository", "https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin")
  }
}

val kotlinVersion: String = project.property("kotlinVersion") as String
// Below not working for some reason
//val kotlinVersion: String by project.properties
val junitPlatformVersion: String by rootProject.extra
val junitTestImplementationArtifacts: Map<String, Map<String, String>> by rootProject.extra
val junitTestRuntimeOnlyArtifacts: Map<String, Map<String, String>> by rootProject.extra

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
  sourceSets.invoke {
    // This source set used for resources to get IDE completion for ease of writing tests against JenkinsPipelineUnit and Jenkins Test Harness
    val pipelineTestResources by creating {
      java.setSrcDirs(emptyList<Any>())
      kotlin.setSrcDirs(emptyList<Any>())
      resources.setSrcDirs(listOf(file("src/$name")))
    }

    "test" {
      runtimeClasspath += pipelineTestResources.output
    }
  }
}

repositories {
  jcenter()
  maven(url = "https://repo.jenkins-ci.org/public/")
}

val SourceSet.kotlin: SourceDirectorySet
  get() = withConvention(KotlinSourceSet::class) { kotlin }

dependencies {
  api(gradleApi())
  api("com.squareup", "javapoet", "1.9.0")
  implementation("io.github.microutils:kotlin-logging:1.4.6")
  testImplementation(kotlin("reflect", embeddedKotlinVersion))
  testImplementation("com.mkobit.gradle.test:gradle-test-kotlin-extensions:0.1.0")
  testImplementation("com.mkobit.gradle.test:assertj-gradle:0.2.0")
  testImplementation("com.google.guava:guava:23.0")
  testImplementation("org.assertj:assertj-core:3.8.0")
  testImplementation("com.nhaarman:mockito-kotlin:1.5.0")
  junitTestImplementationArtifacts.values.forEach {
    testImplementation(it)
  }
  junitTestRuntimeOnlyArtifacts.values.forEach {
    testRuntimeOnly(it)
  }

  // These are used for code completion in the pipelineTestResources to more easily facilitate writing tests
  // against the libraries that are used.
  val pipelineTestResources by java.sourceSets.getting
  pipelineTestResources.compileOnlyConfigurationName("com.lesfurets:jenkins-pipeline-unit:1.1")
  pipelineTestResources.compileOnlyConfigurationName("org.jenkins-ci.main:jenkins-test-harness:2.31")
  pipelineTestResources.compileOnlyConfigurationName("org.codehaus.groovy:groovy:2.4.11")
  // TODO: have to figure out a better way to manage these dependencies (and transitives)
  // TODO: figure out why failing in IntelliJ
//  val jenkinsPluginDependencies = listOf(
//    "org.jenkins-ci.plugins:git:3.5.1",
//    "org.jenkins-ci.plugins.workflow:workflow-api:2.22",
//    "org.jenkins-ci.plugins.workflow:workflow-basic-steps:2.6",
//    "org.jenkins-ci.plugins.workflow:workflow-cps:2.40",
//    "org.jenkins-ci.plugins.workflow:workflow-cps-global-lib:2.9",
//    "org.jenkins-ci.plugins.workflow:workflow-durable-task-step:2.15",
//    "org.jenkins-ci.plugins.workflow:workflow-job:2.14.1",
//    "org.jenkins-ci.plugins.workflow:workflow-multibranch:2.16",
//    "org.jenkins-ci.plugins.workflow:workflow-scm-step:2.6",
//    "org.jenkins-ci.plugins.workflow:workflow-step-api:2.13",
//    "org.jenkins-ci.plugins.workflow:workflow-support:2.15"
//  )
//  jenkinsPluginDependencies.forEach {
//    pipelineTestResources.compileOnlyConfigurationName(it) {
//      artifact {
//        name = this@compileOnlyConfigurationName.name
//        extension = "jar"
//      }
//      isTransitive = true
//    }
//  }
  pipelineTestResources.compileOnlyConfigurationName("org.jenkins-ci.main:jenkins-core:2.73.2") {
    isTransitive = false
  }
}

extensions.getByType(JUnitPlatformExtension::class.java).apply {
  platformVersion = junitPlatformVersion
  filters {
    engines {
      include("junit-jupiter")
    }
  }
  logManager = "org.apache.logging.log4j.jul.LogManager"
  details = Details.TREE
}

tasks {
  withType<Jar> {
    manifest {
      attributes(mapOf(
        "Build-Revision" to gitCommitSha,
        "Implementation-Version" to project.version
      ))
    }
  }

  withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
  }

  "wrapper"(Wrapper::class) {
    gradleVersion = "4.3"
    distributionType = Wrapper.DistributionType.ALL
  }

  "downloadDependencies" {
    val downloadedDependenciesIndex = file("$buildDir/downloadedDependencies.txt")
    description = "Downloads dependencies for caching and usage on Circle CI"
    configurations.filter { it.isCanBeResolved }.forEach { inputs.files(it) }
    outputs.file(downloadedDependenciesIndex)
    doFirst {
      val fileNames = configurations.filter { it.isCanBeResolved }.flatMap {
        logger.info("Resolving configuration named ${it.name}")
        it.resolve()
      }.map {
        it.name
      }.joinToString(separator = System.lineSeparator())
      downloadedDependenciesIndex.bufferedWriter().use { it.write(fileNames) }
    }
  }

  "junitPlatformTest"(JavaExec::class) {
    systemProperty("com.mkobit.gradle.test.testkit.runner.DefaultRunnerConfigurer.stacktrace", "full-stacktrace")
    jvmArgs("-XshowSettings:vm", "-XX:+PrintGCTimeStamps", "-XX:+UseG1GC", "-Xmx1g", "-Xms512m")
  }

  val circleCiScriptDestination = file("$buildDir/circle/circleci")
  val downloadCircleCiScript by creating(Exec::class) {
    description = "Download the Circle CI binary"
    val downloadUrl = "https://circle-downloads.s3.amazonaws.com/releases/build_agent_wrapper/circleci"
    inputs.property("url", downloadUrl)
    outputs.file(circleCiScriptDestination)
    doFirst { circleCiScriptDestination.parentFile.mkdirsOrFail() }
    commandLine("curl", "--fail", "-L", downloadUrl, "-o", circleCiScriptDestination)
    doLast { project.exec { commandLine("chmod", "+x", circleCiScriptDestination) } }
  }

  val checkCircleConfig by creating(Exec::class) {
    description = "Checks that the Circle configuration is valid"
    // Disabled until https://discuss.circleci.com/t/allow-for-using-circle-ci-tooling-without-a-tty/15501
    enabled = false
    dependsOn(downloadCircleCiScript)
    val circleConfig = file(".circleci/config.yml")
    executable(circleCiScriptDestination)
    args("config", "validate", "-c", circleConfig)
  }

  val circleCiBuild by creating(Exec::class) {
    description = "Runs a build using the local Circle CI configuration"
    // Disabled until https://discuss.circleci.com/t/allow-for-using-circle-ci-tooling-without-a-tty/15501
    enabled = false
    dependsOn(downloadCircleCiScript)
    executable(circleCiScriptDestination)
    args("build")
  }

  val main by java.sourceSets
  val sourcesJar by creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles a JAR of the source code"
    classifier = "sources"
    from(main.allSource)
  }

  // No Java code, so don't need the javadoc task.
  // Dokka generates our documentation.
  remove(getByName("javadoc"))
  val dokka by getting(DokkaTask::class) {
    dependsOn(main.classesTaskName)
    outputFormat = "html"
    outputDirectory = "$buildDir/javadoc"
    sourceDirs = main.kotlin.srcDirs
  }

  val javadocJar by creating(Jar::class) {
    dependsOn(dokka)
    description = "Assembles a JAR of the generated Javadoc"
    from(dokka.outputDirectory)
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    classifier = "javadoc"
  }

  val assemble by getting {
    dependsOn(sourcesJar, javadocJar)
  }

  val login by getting

  val gitDirtyCheck by creating {
    doFirst {
      val output = ByteArrayOutputStream().use {
        exec {
          commandLine("git", "status", "--porcelain")
          standardOutput = it
        }
        it.toString(Charsets.UTF_8.name()).trim()
      }
      if (output.isNotBlank()) {
        throw GradleException("Workspace is dirty:\n$output")
      }
    }
  }

  val docVersionChecks by creating {
    group = PublishingPlugin.PUBLISH_TASK_GROUP
    description = "Checks if the repository documentation is up-to-date for the version $version"
    val readme = file("README.adoc")
    val changeLog = file("CHANGELOG.adoc")
    inputs.file(readme)
    inputs.file(changeLog)
    // Output is just used for up-to-date checking
    outputs.dir(file("$buildDir/repositoryDocumentation"))
    doFirst {
      readme.bufferedReader().use { it.readText() }.let { text ->
        val versionAttribute = ":latest-version: $version"
        val containsVersionAttribute = text.contains(versionAttribute)
        if (!containsVersionAttribute) {
          throw GradleException("$readme does not contain up-to-date :latest-version: attribute. Should contain '$versionAttribute'")
        }
      }
      changeLog.bufferedReader().use { it.readLines() }.let { lines ->
        val changelogLineRegex = Regex("^== ${version.toString().replace(".", "\\.")} \\(\\d{4}\\/\\d{2}\\/\\d{2}\\)\$")
        val changelogSectionMatch = lines.any { line -> changelogLineRegex.matches(line) }
        if (!changelogSectionMatch) {
          throw GradleException("$changeLog does not contain section for $version")
        }
      }
    }
  }

  // TODO: use a better release plugin
  val gitTag by creating(Exec::class) {
    group = PublishingPlugin.PUBLISH_TASK_GROUP
    description = "Tags the local repository with version ${project.version}"
    commandLine("git", "tag", "-a", project.version, "-m", "Gradle created tag for ${project.version}")
  }

  val publishPlugins by getting {
    dependsOn(gitDirtyCheck)
    mustRunAfter(login, docVersionChecks)
  }

  val pushGitTag by creating(Exec::class) {
    dependsOn(gitDirtyCheck)
    description = "Pushes Git tag ${project.version} to origin"
    group = PublishingPlugin.PUBLISH_TASK_GROUP
    mustRunAfter(publishPlugins, gitTag, docVersionChecks)
    commandLine("git", "push", "origin", "refs/tags/${project.version}")
  }

  "release" {
    group = PublishingPlugin.PUBLISH_TASK_GROUP
    description = "Publishes the plugin to the Gradle plugin portal and pushes up a Git tag for the current commit"
    dependsOn(docVersionChecks, publishPlugins, pushGitTag, gitTag, gitDirtyCheck, "build")
  }
}

artifacts {
  val sourcesJar by tasks.getting
  val javadocJar by tasks.getting
  add("archives", sourcesJar)
  add("archives", javadocJar)
}

val sharedLibraryPluginId = "com.mkobit.jenkins.pipelines.shared-library"
gradlePlugin {
  plugins.invoke {
    // Don't get the extensions for NamedDomainObjectContainer here because we only have a NamedDomainObjectContainer
    // See https://github.com/gradle/kotlin-dsl/issues/459
    "sharedLibrary" {
      id = sharedLibraryPluginId
      implementationClass = "com.mkobit.jenkins.pipelines.SharedLibraryPlugin"
    }
  }
}

pluginBundle {
  vcsUrl = "https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin"
  description = "Configures and sets up a Gradle project for development and testing of a Jenkins Pipeline shared library (https://jenkins.io/doc/book/pipeline/shared-libraries/)"
  tags = listOf("jenkins", "pipeline", "shared library", "global library")
  website = "https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin"

  plugins(delegateClosureOf<NamedDomainObjectContainer<PluginConfig>> {
    invoke {
      "pipelineLibraryDevelopment" {
        id = sharedLibraryPluginId
        displayName = "Jenkins Pipeline Shared Library Development"
      }
    }
  })
}
