import buildsrc.ProjectInfo
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL

plugins {
  `kotlin-dsl`
  `java-library`
  `maven-publish`

  // Only used for local publishing for testing
  buildsrc.`jenkins-rebaseline`

  alias(libs.plugins.ktlint)
  alias(libs.plugins.nebula.release)
  alias(libs.plugins.gradlePluginPublish)
  alias(libs.plugins.dokka)
  alias(libs.plugins.benManesVersions)

  alias(libs.plugins.rewrite)
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
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
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

ktlint {
  coloredOutput.set(false)
  ignoreFailures.set(true)
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
        "org.junit.jupiter" -> useVersion("5.5.1")
        "org.junit.platform" -> useVersion("1.5.1")
        "io.strikt" -> useVersion("0.21.1")
        "org.apache.logging.log4j" -> useVersion("2.12.0")
      }
    }
  }
}

dependencies {
  api(gradleApi())
  api("com.squareup:javapoet:1.12.1")
  implementation("io.github.microutils:kotlin-logging:1.7.10")
  implementation("com.squareup.okhttp3:okhttp")

  testImplementation(kotlin("reflect"))
  testImplementation("com.mkobit.gradle.test:gradle-test-kotlin-extensions:0.7.0")
  testImplementation("io.mockk:mockk:1.10.0")
  testImplementation("com.google.guava:guava:29.0-jre")
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

  rewrite(platform(libs.rewrite.recipes.bom))
  rewrite(libs.bundles.rewrite.recipes)
}

tasks {
  wrapper {
    gradleVersion = "7.6.4"
  }

  dependencyUpdates {
    val rejectPatterns =
      listOf("alpha", "beta", "rc", "cr", "m").map { qualifier ->
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
          "Build-Revision" to gitCommitShaProvider.get(),
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
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_17)
    }
  }

  test {
    useJUnitPlatform()
    systemProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")
    project.findProperty("gradleTestVersions")?.let {
      // Will rerun some tests unfortunately using this method, but helps with CI
      systemProperty("testsupport.junit.ForGradleVersions.versions", it)
    }
    jvmArgs("-XshowSettings:vm", "-Xlog:gc*", "-Xmx512m", "-Xms256m")
    testLogging {
      if (env("CI") != null) {
        // shoot more output out so that Circle CI doesn't kill build after no output in 10 minutes
        events(TestLogEvent.FAILED, TestLogEvent.SKIPPED, TestLogEvent.PASSED)
      } else {
        events(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
      }
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

val pluginTags = listOf("jenkins", "pipeline", "shared library", "global library")

gradlePlugin {
  website = ProjectInfo.projectUrl
  vcsUrl = ProjectInfo.projectUrl
  plugins {
    // Don't get the extensions for NamedDomainObjectContainer here because we only have a NamedDomainObjectContainer
    // See https://github.com/gradle/kotlin-dsl/issues/459
    create("sharedLibrary") {
      id = "com.mkobit.jenkins.pipelines.shared-library"
      implementationClass = "com.mkobit.jenkins.pipelines.SharedLibraryPlugin"
      displayName = "Jenkins Pipeline Shared Library Development"
      description =
        "Configures and sets up a Gradle project for development and testing of a Jenkins Pipeline shared library (https://jenkins.io/doc/book/pipeline/shared-libraries/)"
      tags.set(pluginTags)
    }
    create("jenkinsIntegration") {
      id = "com.mkobit.jenkins.pipelines.jenkins-integration"
      implementationClass = "com.mkobit.jenkins.pipelines.JenkinsIntegrationPlugin"
      displayName = "Jenkins Integration Plugin"
      description =
        "Tasks to retrieve information from a Jenkins instance to be aid in the development of tools with Gradle"
      tags.set(pluginTags)
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

fun env(key: String): String? = System.getenv(key)

develocity {
  buildScan {
    termsOfUseUrl = "https://gradle.com/terms-of-service"
    termsOfUseAgree = "yes"

    if (!env("CI").isNullOrEmpty()) {
      publishing.onlyIf { true }

      logger.lifecycle("Running in CI environment, setting build scan attributes.")
      tag("CI")

      // Env variables from https://circleci.com/docs/2.0/env-vars/
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
}
