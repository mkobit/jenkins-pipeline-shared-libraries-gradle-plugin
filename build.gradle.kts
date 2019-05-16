import buildsrc.ProjectInfo
import org.gradle.jvm.tasks.Jar
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream
import java.net.URL

plugins {
  id("com.gradle.build-scan") version "2.3"
  `kotlin-dsl`
  id("org.jlleitschuh.gradle.ktlint") version "8.0.0"
  `java-library`
  id("com.gradle.plugin-publish") version "0.10.1"
  id("com.github.ben-manes.versions") version "0.21.0"
  id("org.jetbrains.dokka") version "0.9.18"
  // Only used for local publishing for testing
  `maven-publish`
  buildsrc.`jenkins-rebaseline`
}

version = "0.8.0"
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

buildScan {
  fun env(key: String): String? = System.getenv(key)

  termsOfServiceAgree = "yes"
  termsOfServiceUrl = "https://gradle.com/terms-of-service"

  // Env variables from https://circleci.com/docs/2.0/env-vars/
  if (env("CI") != null) {
    logger.lifecycle("Running in CI environment, setting build scan attributes.")
    tag("CI")
    env("CIRCLE_BRANCH")?.let { tag(it) }
    env("CIRCLE_BUILD_NUM")?.let { value("Circle CI Build Number", it) }
    env("CIRCLE_BUILD_URL")?.let { link("Build URL", it) }
    env("CIRCLE_SHA1")?.let { value("Revision", it) }
    //    Issue with Circle CI/Gradle with caret (^) in URLs
//    see: https://discuss.gradle.org/t/build-scan-plugin-1-10-3-issue-when-using-a-url-with-a-caret/24965
//    see: https://discuss.circleci.com/t/circle-compare-url-does-not-url-escape-caret/18464
//    env("CIRCLE_COMPARE_URL")?.let { link("Diff", it) }
    env("CIRCLE_REPOSITORY_URL")?.let { value("Repository", it) }
    env("CIRCLE_PR_NUMBER")?.let { value("Pull Request Number", it) }
    link("Repository", ProjectInfo.projectUrl)
  }
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
  maven(url = "https://repo.jenkins-ci.org/public/")
  jcenter()
}

ktlint {
  version.set("0.32.0")
}

configurations {
  // This is very similar to the dependency resolution used in the plugin implementation.
  // It is mainly used to get IDEA autocompletion and for use it caching dependencies in Circle CI
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
        "dev.minutest" -> useVersion("1.7.0")
        "org.junit.jupiter" -> useVersion("5.4.2")
        "org.junit.platform" -> useVersion("1.4.2")
        "io.strikt" -> useVersion("0.20.1")
        "org.apache.logging.log4j" -> useVersion("2.11.2")
      }
    }
  }
}

dependencies {
  api(gradleApi())
  api("com.squareup:javapoet:1.11.1")
  implementation("io.github.microutils:kotlin-logging:1.6.26")
  implementation("com.squareup.okhttp3:okhttp")
  testImplementation(kotlin("reflect"))
  testImplementation("org.assertj:assertj-core:3.12.2")
  testImplementation("com.mkobit.gradle.test:assertj-gradle:0.2.0")
  testImplementation("com.mkobit.gradle.test:gradle-test-kotlin-extensions:0.6.0")
  testImplementation("com.google.guava:guava:27.1-jre")
  testImplementation("org.mockito:mockito-core:2.27.0")
  testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.1.0")
  testImplementation("com.squareup.okhttp3:mockwebserver")

  testImplementation("io.strikt:strikt-core")
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
  pipelineTestResources.compileOnlyConfigurationName("org.jenkins-ci.main:jenkins-test-harness:2.44")
  pipelineTestResources.compileOnlyConfigurationName("org.codehaus.groovy:groovy:2.4.12")
  val jenkinsPluginDependencies = listOf(
    "org.jenkins-ci.plugins.workflow:workflow-api:2.33",
    "org.jenkins-ci.plugins.workflow:workflow-basic-steps:2.13",
    "org.jenkins-ci.plugins.workflow:workflow-cps:2.61",
    "org.jenkins-ci.plugins.workflow:workflow-cps-global-lib:2.12",
    "org.jenkins-ci.plugins.workflow:workflow-durable-task-step:2.26",
    "org.jenkins-ci.plugins.workflow:workflow-job:2.29",
    "org.jenkins-ci.plugins.workflow:workflow-multibranch:2.20",
    "org.jenkins-ci.plugins.workflow:workflow-scm-step:2.7",
    "org.jenkins-ci.plugins.workflow:workflow-step-api:2.16",
    "org.jenkins-ci.plugins.workflow:workflow-support:2.23"
  )
  jenkinsPluginDependencies.forEach {
    "jenkinsPlugins"(it)
  }
  "jenkinsPlugins"("org.jenkins-ci.main:jenkins-core:2.138.3") {
    isTransitive = false
  }
}

tasks {
  wrapper {
    gradleVersion = "5.4.1"
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
      attributes(mapOf(
        "Build-Revision" to gitCommitSha,
        "Implementation-Version" to project.version
      ))
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
    findProperty("gradleTestVersions")?.let {
      // Will rerun some tests unfortunately using this method, but helps with CI
      systemProperty("testsupport.ForGradleVersions.versions", it)
    }
    jvmArgs("-XshowSettings:vm", "-XX:+PrintGCTimeStamps", "-XX:+UseG1GC", "-Xmx512m", "-Xms256m")
    testLogging {
      events("skipped", "failed")
    }
  }

  val circleCiBundleDownloadLocation = layout.buildDirectory.file("circle/circleci.tar.gz")
  val circleCiBundleUnpackLocation = layout.buildDirectory.dir("circle/circle-unpacked")
  val circleCiScriptDestination = circleCiBundleUnpackLocation.map {
    fileTree(it).filter { fileDetails -> fileDetails.name == "circleci" }.singleFile
  }
  val downloadCircleCiBundle by registering {
    val downloadUrl = "https://github.com/CircleCI-Public/circleci-cli/releases/download/v0.1.5607/circleci-cli_0.1.5607_linux_amd64.tar.gz"
    inputs.property("url", downloadUrl)
    outputs.file(circleCiBundleDownloadLocation)
    doFirst("download archive") {
      ant.invokeMethod("get", mapOf("src" to downloadUrl, "dest" to circleCiBundleDownloadLocation.map { it.asFile }.get()))
    }
  }

  val unpackCircleCi by registering {
    dependsOn(downloadCircleCiBundle)
    inputs.file(circleCiBundleDownloadLocation)
    outputs.dir(circleCiBundleUnpackLocation)
    doFirst("unpack archive") {
      copy {
        from(tarTree(resources.gzip(circleCiBundleDownloadLocation.map { it.asFile }.get())))
        into(circleCiBundleUnpackLocation)
      }
      ant.invokeMethod("chmod", mapOf("file" to circleCiScriptDestination.get(), "perm" to "+x"))
    }
  }

  register("checkCircleConfig") {
    description = "Checks that the Circle configuration is valid"
    dependsOn(unpackCircleCi)
    doFirst("execute circle config validate") {
      exec {
        executable(circleCiScriptDestination.get())
        args("config", "validate", "-c", file(".circleci/config.yml"))
      }
    }
  }

  register("circleCiBuild") {
    description = "Runs a build using the local Circle CI configuration"
    // Fails with workflows - https://discuss.circleci.com/t/command-line-support-for-workflows/14510
    enabled = false
    dependsOn(unpackCircleCi)
    doFirst("execute circle build") {
      exec {
        executable(circleCiScriptDestination.get())
        args("build")
      }
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
    val changeLog = file("CHANGELOG.adoc")
    inputs.file(changeLog)
    // Output is just used for up-to-date checking
    outputs.dir(file("$buildDir/repositoryDocumentation"))
    doFirst {
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
    commandLine("git", "tag", "--sign", "--annotate", project.version, "--message", "Gradle created tag for ${project.version}")
  }

  publishPlugins {
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

  register("release") {
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

pluginBundle {
  vcsUrl = ProjectInfo.projectUrl
  tags = listOf("jenkins", "pipeline", "shared library", "global library")
  website = ProjectInfo.projectUrl
}
