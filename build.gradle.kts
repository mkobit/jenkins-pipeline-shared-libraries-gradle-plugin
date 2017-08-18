import com.gradle.publish.PluginConfig
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.junit.platform.console.options.Details
import org.junit.platform.gradle.plugin.JUnitPlatformExtension

buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    // TODO: load from properties or script plugin
    classpath("org.junit.platform:junit-platform-gradle-plugin:1.0.0-RC2")
  }
}

plugins {
  kotlin("jvm")
//  `kotlin-dsl`
  `java-library`
  `java-gradle-plugin`
  id("com.gradle.plugin-publish") version "0.9.7"
  id("com.github.ben-manes.versions") version "0.15.0"
}

apply {
  plugin("org.junit.platform.gradle.plugin")
  from("gradle/junit5.gradle.kts")
}

version = "0.1.0"
group = "com.mkobit.jenkins.pipelines"

val kotlinVersion: String = project.property("kotlinVersion") as String
// Below not working for some reason
//val kotlinVersion: String by project.properties
val junitPlatformVersion: String by rootProject.extra
val junitTestImplementationArtifacts: Map<String, Map<String, String>> by rootProject.extra
val junitTestRuntimeOnlyArtifacts: Map<String, Map<String, String>> by rootProject.extra

repositories {
  jcenter()
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
  api(gradleApi())
  implementation(kotlin("stdlib-jre8", kotlinVersion))
  testImplementation(kotlin("reflect", kotlinVersion))
  testImplementation("com.google.guava:guava:23.0")
  testImplementation("org.assertj:assertj-core:3.8.0")
  testImplementation("org.eclipse.jgit:org.eclipse.jgit.junit:4.8.0.201706111038-r")
  testImplementation("com.nhaarman:mockito-kotlin:1.5.0")
  junitTestImplementationArtifacts.values.forEach {
    testImplementation(it)
  }
  junitTestRuntimeOnlyArtifacts.values.forEach {
    testRuntimeOnly(it)
  }
}

tasks.withType(KotlinCompile::class.java) {
  kotlinOptions.jvmTarget = "1.8"
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
  description = "Configures and sets up a pipeline project for development and testing of a shared library created for https://jenkins.io/doc/book/pipeline/shared-libraries/"
  tags = listOf("jenkins", "pipeline", "shared library", "global library")

  plugins(delegateClosureOf<NamedDomainObjectContainer<PluginConfig>> {
    this {
      "pipelineLibraryDevelopment" {
        id = "com.mkobit.jenkins.pipelines.shared-library"
        displayName = "Jenkins Pipeline Shared Library Development"
      }
    }
  })
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
