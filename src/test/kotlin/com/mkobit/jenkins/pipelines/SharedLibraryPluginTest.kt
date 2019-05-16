package com.mkobit.jenkins.pipelines

import com.mkobit.jenkins.pipelines.codegen.GenerateJavaFile
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.assertj.core.description.TextDescription
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.HasConvention
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.GroovySourceSet
import org.gradle.jvm.tasks.Jar
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import testsupport.NotImplementedYet
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
    assertThat(project.pluginManager.hasPlugin("groovy"))
      .describedAs("'groovy' plugin applied")
      .isTrue()
  }

  @Test
  internal fun `JenkinsIntegrationPlugin is applied`() {
    assertThat(project.plugins.hasPlugin(JenkinsIntegrationPlugin::class.java))
      .describedAs("${JenkinsIntegrationPlugin::class.simpleName} is applied")
      .isTrue()
  }

  @Test
  internal fun `Jenkins repository is added`() {
    val repository = project.repositories.getByName(SharedLibraryPlugin.JENKINS_REPOSITORY_NAME)
    assertThat(repository)
      .isInstanceOf(MavenArtifactRepository::class.java)
      .isNotNull()
    assertThat(repository as MavenArtifactRepository)
      .satisfies { mavenArtifactRepository ->
        assertThat(mavenArtifactRepository.url)
          .hasAuthority("repo.jenkins-ci.org")
          .hasScheme("https")
        assertThat(mavenArtifactRepository.name)
          .`as`("Has name \"JenkinsPublic\"")
          .isEqualTo("JenkinsPublic")
      }
  }

  @Test
  internal fun `sourceCompatibility is Java 8`() {
    val convention = project.convention.getPlugin(JavaPluginConvention::class.java)

    assertThat(convention.sourceCompatibility).isEqualTo(JavaVersion.VERSION_1_8)
    assertThat(convention.targetCompatibility).isEqualTo(JavaVersion.VERSION_1_8)
  }

  @Test
  internal fun `src is a Groovy source directory`() {
    val convention = project.convention.getPlugin(JavaPluginConvention::class.java)
    val main = convention.sourceSets.getByName("main")
    assertThat(main).isNotNull()
    assertThat((main as HasConvention).convention.getPlugin(GroovySourceSet::class.java).groovy.srcDirs).anySatisfy {
      assertThat(it.endsWith("src"))
    }
  }

  @Test
  internal fun `vars is a Groovy source directory`() {
    val convention = project.convention.getPlugin(JavaPluginConvention::class.java)
    val main = convention.sourceSets.getByName("main")
    assertThat(main).isNotNull()
    assertThat((main as HasConvention).convention.getPlugin(GroovySourceSet::class.java).groovy.srcDirs).anySatisfy {
      assertThat(it.endsWith("vars"))
    }
  }

  @Test
  internal fun `src is a resources directory`() {
    val convention = project.convention.getPlugin(JavaPluginConvention::class.java)
    val main = convention.sourceSets.getByName("main")
    assertThat(main).isNotNull()
    assertThat(main.resources.srcDirs).hasOnlyOneElementSatisfying {
      assertThat(it.endsWith("resources"))
    }
  }

  @Test
  internal fun `main has no Java sources`() {
    val convention = project.convention.getPlugin(JavaPluginConvention::class.java)
    val main = convention.sourceSets.getByName("main")
    assertThat(main).isNotNull()
    assertThat(main.java.srcDirs).isEmpty()
  }

  @Test
  internal fun `main implementation configuration extends from Shared Library Groovy configuration`() {
    val implementation = project.configurations.getByName("implementation")

    assertThat(implementation.extendsFrom.map { it.name })
      .contains("sharedLibraryGroovy")
  }

  @Test
  internal fun `integrationTest task sets the system property for the buildDirectory`() {
    val integrationTest = project.tasks.getByName("integrationTest")

    assertThat(integrationTest).isNotNull().isInstanceOf(org.gradle.api.tasks.testing.Test::class.java)
    assertThat((integrationTest as org.gradle.api.tasks.testing.Test).systemProperties).hasEntrySatisfying("buildDirectory") {
      assertThat(it).isEqualTo(project.buildDir.absolutePath)
    }
  }

  @Test
  internal fun `integrationTest task is in the verification group`() {
    val integrationTest = project.tasks.getByName("integrationTest")

    assertThat(integrationTest).isNotNull().isInstanceOf(org.gradle.api.tasks.testing.Test::class.java)
    assertThat(integrationTest.group).isEqualTo(JavaBasePlugin.VERIFICATION_GROUP)
    assertThat(integrationTest.description).isNotNull()
  }

  @Test
  internal fun `groovydocJar task is created`() {
    val groovydocJar = project.tasks.getByName("groovydocJar")
    assertThat(groovydocJar).satisfies {
      assertThat(it)
        .isNotNull()
        .isInstanceOf(Jar::class.java)
      assertThat(it.description).isNotEmpty()
    }
  }

  @Test
  internal fun `sourcesJar task is created`() {
    val sourcesJar = project.tasks.getByName("sourcesJar")
    assertThat(sourcesJar).satisfies {
      assertThat(it)
        .isNotNull()
        .isInstanceOf(Jar::class.java)
      assertThat(it.description).isNotEmpty()
    }
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
        DynamicTest.dynamicTest("for $value has a description and is not visible") {
          val configuration = project.configurations.getByName(key)
          SoftAssertions.assertSoftly {
            val description = TextDescription("Configuration '%s'", key)
            assertThat(configuration)
              .describedAs(description)
              .isNotNull
            assertThat(configuration.description).describedAs(description).isNotEmpty()
            assertThat(configuration.isVisible).describedAs(description).isFalse()
          }
        }
      }
  }

  @Test
  internal fun `code generation tasks do not have a group`() {
    val generationTasks = project.tasks.withType(GenerateJavaFile::class.java)

    assertThat(generationTasks)
      .isNotEmpty
      .allSatisfy {
        assertThat(it.group)
          .`as`("Group is not set for ${GenerateJavaFile::class} tasks")
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
