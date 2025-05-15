package com.mkobit.jenkins.pipelines

import com.mkobit.jenkins.pipelines.codegen.GenerateJavaFile
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.HasConvention
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.GroovySourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.any
import strikt.assertions.contains
import strikt.assertions.endsWith
import strikt.assertions.first
import strikt.assertions.hasEntry
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotBlank
import strikt.assertions.isNotEmpty
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.map
import strikt.java.name
import testsupport.junit.Issue
import testsupport.junit.NotImplementedYet
import testsupport.strikt.authority
import testsupport.strikt.scheme
import java.util.stream.Stream

internal class SharedLibraryPluginTest {
  private lateinit var project: Project

  @BeforeEach
  internal fun setUp() {
    project = ProjectBuilder.builder().build()
    project.pluginManager.apply(SharedLibraryPlugin::class.java)
  }

  @Test
  internal fun `Groovy plugin is applied`() {
    expectThat(project)
      .get { pluginManager }
      .assertThat("'groovy' plugin applied") { it.hasPlugin("groovy") }
  }

  @Test
  internal fun `JenkinsIntegrationPlugin is applied`() {
    expectThat(project)
      .get { plugins }
      .assertThat("${JenkinsIntegrationPlugin::class.simpleName} is applied") { it.hasPlugin(JenkinsIntegrationPlugin::class.java) }
  }

  @Test
  @Issue("https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/101")
  internal fun `Jenkins repository is added`() {
    expectThat(project)
      .get { repositories }
      .and {
        get { size }.describedAs("a single repository is added").isEqualTo(1)
      }
      .get("repository named '${SharedLibraryPlugin.JENKINS_REPOSITORY_NAME}'") { getByName(SharedLibraryPlugin.JENKINS_REPOSITORY_NAME) }
      .isA<MavenArtifactRepository>()
      .and {
        get { url }.and {
          authority.isEqualTo("repo.jenkins-ci.org")
          scheme.isEqualTo("https")
        }
        get { name }.isEqualTo("JenkinsPublic")
      }
  }

  @Test
  internal fun `sourceCompatibility is Java 8`() {
    expectThat(project)
      .get { convention }
      .get("Java Plugin convention") { getPlugin(JavaPluginConvention::class.java) }
      .and {
        get { sourceCompatibility }.isEqualTo(JavaVersion.VERSION_1_8)
        get { targetCompatibility }.isEqualTo(JavaVersion.VERSION_1_8)
      }
  }

  @Test
  internal fun `src is a Groovy source directory`() {
    expectThat(project)
      .get { extensions }
      .get("Source Sets containers") { getByType(SourceSetContainer::class.java) }
      .get("main") { getByName("main") }
      .isA<HasConvention>()
      .get { convention }
      .get { getPlugin(GroovySourceSet::class.java) }
      .get { groovy }
      .get { srcDirs }
      .any {
        name.endsWith("src")
      }
//    val convention = project.convention.getPlugin(JavaPluginConvention::class.java)
//    val main = convention.sourceSets.getByName("main")
//    expectThat(main).isNotNull()
//    expectThat((main as HasConvention).convention.getPlugin(GroovySourceSet::class.java).groovy.srcDirs).anySatisfy {
//      expectThat(it.endsWith("src"))
//    }
  }

  @Test
  internal fun `vars is a Groovy source directory`() {
    expectThat(project)
      .get { extensions }
      .get("Source Sets containers") { getByType(SourceSetContainer::class.java) }
      .get("main") { getByName("main") }
      .isA<HasConvention>()
      .get { convention }
      .get { getPlugin(GroovySourceSet::class.java) }
      .get { groovy }
      .get { srcDirs }
      .any {
        name.endsWith("vars")
      }
  }

  @Test
  internal fun `resources is a resources source directory`() {
    expectThat(project)
      .get { extensions }
      .get("Source Sets containers") { getByType(SourceSetContainer::class.java) }
      .get("main") { getByName("main") }
      .get { resources }
      .get { srcDirs }
      .hasSize(1)
      .first()
      .and {
        name.endsWith("resources")
      }
  }

  @Test
  internal fun `main has no Java sources`() {
    expectThat(project)
      .get { extensions }
      .get("Source Sets containers") { getByType(SourceSetContainer::class.java) }
      .get("main") { getByName("main") }
      .get { java }
      .get { srcDirs }
      .isEmpty()
  }

  @Test
  internal fun `main implementation configuration extends from Shared Library Groovy configuration`() {
    expectThat(project)
      .get { configurations }
      .get { getByName("implementation") }
      .get { extendsFrom }
      .map { it.name }
      .contains("sharedLibraryGroovy")
  }

  @Test
  internal fun `integrationTest task sets the system property for the buildDirectory`() {
    expectThat(project)
      .get { tasks }
      .get { getByName("integrationTest") }
      .isA<org.gradle.api.tasks.testing.Test>()
      .get { systemProperties }
      .hasEntry("buildDirectory", project.buildDir.absolutePath)
  }

  @Test
  internal fun `integrationTest task is in the verification group`() {
    expectThat(project)
      .get { tasks }
      .get { getByName("integrationTest") }
      .isA<org.gradle.api.tasks.testing.Test>()
      .and {
        get { group }.isEqualTo(JavaBasePlugin.VERIFICATION_GROUP)
        get { description }.isNotBlank()
      }
  }

  @Test
  internal fun `groovydocJar task is created`() {
    expectThat(project)
      .get { tasks }
      .get { getByName("groovydocJar") }
      .isA<Jar>()
      .get { description }
      .isNotBlank()
  }

  @Test
  internal fun `sourcesJar task is created`() {
    expectThat(project)
      .get { tasks }
      .get { getByName("sourcesJar") }
      .isA<Jar>()
      .get { description }
      .isNotBlank()
  }

  @TestFactory
  internal fun `configuration setup`(): Stream<DynamicNode> {
    val configurations = mapOf(
      "jenkinsPlugins" to "Jenkins Plugins",
      "jenkinsPipelineUnitTestLibraries" to "Jenkins Pipeline Unit dependencies",
      "jenkinsPluginHpisAndJpis" to "Jenkins plugins HPI and JPI dependencies",
      "jenkinsPluginLibraries" to "Jenkins plugins JAR dependencies",
      "jenkinsCoreLibraries" to "Jenkins core dependencies",
      "jenkinsTestLibraries" to "Jenkins test dependencies",
      "sharedLibraryGroovy" to "Shared Library Groovy",
      "sharedLibraryIvy" to "Ivy (@Grab support)",
      "jenkinsWar" to "Jenkins WAR and modules bundle",
      "jenkinsModules" to "Only Jenkins WAR modules",
      "jenkinsOnlyWarExtension" to "Only Jenkins WAR bundle"
    )

    return configurations.entries.stream()
      .map { (key, value) ->
        DynamicTest.dynamicTest("configuration $value has a description and is not visible") {
          expectThat(project)
            .get { this.configurations }
            .get("configuration $key") { getByName(key) }
            .and {
              get { description }.isNotNull().isNotBlank()
              get { isVisible }.isFalse()
            }
        }
      }
  }

  @Test
  internal fun `code generation tasks do not have a group`() {
    expectThat(project)
      .get { tasks }
      .get { withType(GenerateJavaFile::class.java) }
      .isNotEmpty()
      .all {
        get { group as String? } // not sure why it is not nullable right now
          .isNull()
      }
  }

  @NotImplementedYet
  @Test
  internal fun `Jenkins Global Library plugin implementation and HPI dependencies are added`() {
  }

  @NotImplementedYet
  @Test
  internal fun `additional resources directory available for main to be able to use the Jenkins GDSL in IntelliJ`() {
  }

  // TODO: having this is incredibly useful for authoring integration tests with source code completion in global shared libraries
  @NotImplementedYet
  @Test
  internal fun `integrationTestPipelineResources directory is a source set and available on integrationRuntimeOnly classpath`() {
  }

  // Internal function needed here to trigger evaluation
  private fun Project.evaluate() {
    (this as ProjectInternal).evaluate()
  }
}
