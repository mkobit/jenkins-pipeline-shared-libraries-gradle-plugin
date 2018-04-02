import buildsrc.DependencyInfo
import buildsrc.ProjectInfo
import com.gradle.publish.PluginConfig
import com.gradle.publish.PublishPlugin
import org.gradle.api.internal.HasConvention
import org.gradle.jvm.tasks.Jar
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.preprocessor.mkdirsOrFail
import java.io.ByteArrayOutputStream
import java.net.URL

plugins {
  id("com.gradle.build-scan") version "1.13"
  `kotlin-dsl`
  `java-library`
  `java-gradle-plugin`
  id("com.gradle.plugin-publish") version "0.9.10"
  id("com.github.ben-manes.versions") version "0.17.0"
  id("org.jetbrains.dokka") version "0.9.16"
  // TODO: load version from shared location
  // Only used for local publishing for testing
  `maven-publish`
 id("buildsrc.jenkins-rebaseline")
}

version = "0.5.0"
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

  setTermsOfServiceAgree("yes")
  setTermsOfServiceUrl("https://gradle.com/terms-of-service")

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
  maven(url = "https://repo.jenkins-ci.org/public/")
  jcenter()
}

configurations {
  // This is very similar to the dependency resolution used in the plugin implementation.
  // It is mainly used to get IDEA autocompletion and for use it caching dependencies in Circle CI
  // These are used for code completion in the pipelineTestResources to more easily facilitate writing tests
  // against the libraries that are used.
  val pipelineTestResources by java.sourceSets.getting

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
  configurations.all {
    incoming.beforeResolve {
      if (hierarchy.contains(resourcesCompileOnly)) {
        // Trigger the dependency seeding
        jenkinsPlugins.resolve()
      }
    }
  }
}

dependencies {
  api(gradleApi())
  api(DependencyInfo.javapoet)
  implementation(DependencyInfo.kotlinLogging)
  implementation(DependencyInfo.okHttpClient)
  testImplementation(kotlin("reflect"))
  testImplementation(DependencyInfo.okHttpMockServer)
  testImplementation("com.mkobit.gradle.test:gradle-test-kotlin-extensions:0.3.0")
  testImplementation("com.mkobit.gradle.test:assertj-gradle:0.2.0")
  testImplementation(DependencyInfo.guava)
  testImplementation(DependencyInfo.assertJCore)
  testImplementation(DependencyInfo.mockito)
  testImplementation(DependencyInfo.mockitoKotlin)
  DependencyInfo.junitTestImplementationArtifacts.forEach {
    testImplementation(it)
  }
  DependencyInfo.junitTestRuntimeOnlyArtifacts.forEach {
    testRuntimeOnly(it)
  }

  // These are used for code completion in the pipelineTestResources to more easily facilitate writing tests
  // against the libraries that are used.
  val pipelineTestResources by java.sourceSets.getting
  pipelineTestResources.compileOnlyConfigurationName("com.lesfurets:jenkins-pipeline-unit:1.1")
  pipelineTestResources.compileOnlyConfigurationName("org.jenkins-ci.main:jenkins-test-harness:2.34")
  pipelineTestResources.compileOnlyConfigurationName("org.codehaus.groovy:groovy:2.4.11")
  val jenkinsPluginDependencies = listOf(
    "org.jenkins-ci.plugins.workflow:workflow-api:2.26",
    "org.jenkins-ci.plugins.workflow:workflow-basic-steps:2.6",
    "org.jenkins-ci.plugins.workflow:workflow-cps:2.45",
    "org.jenkins-ci.plugins.workflow:workflow-cps-global-lib:2.9",
    "org.jenkins-ci.plugins.workflow:workflow-durable-task-step:2.19",
    "org.jenkins-ci.plugins.workflow:workflow-job:2.17",
    "org.jenkins-ci.plugins.workflow:workflow-multibranch:2.17",
    "org.jenkins-ci.plugins.workflow:workflow-scm-step:2.6",
    "org.jenkins-ci.plugins.workflow:workflow-step-api:2.14",
    "org.jenkins-ci.plugins.workflow:workflow-support:2.18"
  )
  jenkinsPluginDependencies.forEach {
    "jenkinsPlugins"(it)
  }
  "jenkinsPlugins"("org.jenkins-ci.main:jenkins-core:2.89.4") {
    isTransitive = false
  }
}

tasks {
  "wrapper"(Wrapper::class) {
    gradleVersion = "4.6"
    distributionType = Wrapper.DistributionType.ALL
  }

  withType<Jar> {
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

  withType<Javadoc> {
    options {
      header = project.name
      encoding = "UTF-8"
    }
  }

  withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
  }

  "test"(Test::class) {
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

  val circleCiScriptDestination = file("$buildDir/circle/circleci")
  val downloadCircleCiScript by creating(Exec::class) {
    description = "Download the Circle CI binary"
    val downloadUrl = "https://circle-downloads.s3.amazonaws.com/releases/build_agent_wrapper/circleci"
    inputs.property("url", downloadUrl)
    outputs.file(circleCiScriptDestination)
    doFirst { circleCiScriptDestination.parentFile.mkdirsOrFail() }
    commandLine("curl", "--fail", "-L", downloadUrl, "-o", circleCiScriptDestination)
    doLast {
      project.exec { commandLine("chmod", "+x", circleCiScriptDestination) }
      // Hack: replace -it with -i to work in non TTY - https://discuss.circleci.com/t/allow-for-using-circle-ci-tooling-without-a-tty/15501/4
      project.exec { commandLine("sed", "--in-place", "--", "s/run -it/run -i/g", circleCiScriptDestination) }
    }
  }

  val checkCircleConfig by creating(Exec::class) {
    description = "Checks that the Circle configuration is valid"
    dependsOn(downloadCircleCiScript)
    val circleConfig = file(".circleci/config.yml")
    executable(circleCiScriptDestination)
    args("config", "validate", "-c", circleConfig)
  }

  val circleCiBuild by creating(Exec::class) {
    description = "Runs a build using the local Circle CI configuration"
    // Fails with workflows - https://discuss.circleci.com/t/command-line-support-for-workflows/14510
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
    jdkVersion = 8
    outputFormat = "html"
    outputDirectory = "$buildDir/javadoc"
    sourceDirs = main.kotlin.srcDirs
    // See https://github.com/Kotlin/dokka/issues/196
    externalDocumentationLink(delegateClosureOf<DokkaConfiguration.ExternalDocumentationLink.Builder> {
      url = URL("https://docs.gradle.org/current/javadoc/")
    })
    externalDocumentationLink(delegateClosureOf<DokkaConfiguration.ExternalDocumentationLink.Builder> {
      url = URL("https://docs.oracle.com/javase/8/docs/api/")
    })
    externalDocumentationLink(delegateClosureOf<DokkaConfiguration.ExternalDocumentationLink.Builder> {
      url = URL("https://square.github.io/javapoet/javadoc/javapoet/")
    })
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
    "jenkinsIntegration" {
      id =  "com.mkobit.jenkins.pipelines.jenkins-integration"
      implementationClass = "com.mkobit.jenkins.pipelines.JenkinsIntegrationPlugin"
    }
  }
}

pluginBundle {
  vcsUrl = ProjectInfo.projectUrl
  description = "Configures and sets up a Gradle project for development and testing of a Jenkins Pipeline shared library (https://jenkins.io/doc/book/pipeline/shared-libraries/)"
  tags = listOf("jenkins", "pipeline", "shared library", "global library")
  website = ProjectInfo.projectUrl

  plugins(delegateClosureOf<NamedDomainObjectContainer<PluginConfig>> {
    invoke {
      "pipelineLibraryDevelopment" {
        id = sharedLibraryPluginId
        displayName = "Jenkins Pipeline Shared Library Development"
      }
    }
  })
}
