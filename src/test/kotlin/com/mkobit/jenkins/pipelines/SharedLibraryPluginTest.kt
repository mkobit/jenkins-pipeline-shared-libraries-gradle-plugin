package com.mkobit.jenkins.pipelines

import com.mkobit.jenkins.pipelines.codegen.GenerateJavaFile
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Condition
import org.assertj.core.condition.AllOf.allOf
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.HasConvention
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.GroovySourceSet
import org.gradle.jvm.tasks.Jar
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import testsupport.NotImplementedYet
import java.util.function.Predicate

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

  @NotImplementedYet
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
  internal fun `default Groovy dependency is added to implementation configuration`() {
    val implementation = project.configurations.getByName("implementation")
    project.evaluate()

    val group = Condition<Dependency>(Predicate {
      it.group == "org.codehaus.groovy"
    }, "org.codehaus.groovy")
    val name = Condition<Dependency>(Predicate {
      it.name == "groovy"
    }, "groovy")
    val version = Condition<Dependency>(Predicate {
      it.version == "2.4.11"
    }, "2.4.11")
    assertThat(implementation.dependencies).haveExactly(1, allOf(group, name, version))
  }

  @Test
  internal fun `can configure Groovy dependency version`() {
    val groovyVersion = "2.5.0"
    project.extensions.getByType(SharedLibraryExtension::class.java).groovyVersion = groovyVersion
    project.evaluate()

    val group = Condition<Dependency>(Predicate {
      it.group == "org.codehaus.groovy"
    }, "org.codehaus.groovy")
    val name = Condition<Dependency>(Predicate {
      it.name == "groovy"
    }, "groovy")
    val version = Condition<Dependency>(Predicate {
      it.version == groovyVersion
    }, groovyVersion)

    val implementation = project.configurations.getByName("implementation")
    assertThat(implementation.dependencies).haveExactly(1, allOf(group, name, version))
  }


  @DisplayName("JenkinsPipelineUnit default version is 1.1")
  @Test
  internal fun `JenkinsPipelineUnit default version is 1_1`() {
    val pipelineUnitVersion = "1.1"
    project.evaluate()

    val group = Condition<Dependency>(Predicate {
      it.group == "com.lesfurets"
    }, "com.lesfurets")
    val name = Condition<Dependency>(Predicate {
      it.name == "jenkins-pipeline-unit"
    }, "jenkins-pipeline-unit")
    val version = Condition<Dependency>(Predicate {
      it.version == pipelineUnitVersion
    }, pipelineUnitVersion)

    val implementation = project.configurations.getByName("jenkinsPipelineUnitTestLibraries")
    assertThat(implementation.incoming.dependencies).haveExactly(1, allOf(group, name, version))
  }

  @Test
  internal fun `Jenkins Test Harness is available in Jenkins test libraries configuration`() {
    project.evaluate()

    val group = Condition<Dependency>(Predicate {
      it.group == "org.jenkins-ci.main"
    }, "org.jenkins-ci.main")
    val name = Condition<Dependency>(Predicate {
      it.name == "jenkins-test-harness"
    }, "jenkins-test-harness")

    val configuration = project.configurations.getByName("jenkinsTestLibraries")
    assertThat(configuration.incoming.dependencies).haveExactly(1, allOf(group, name))
  }

  @Disabled("may not happen at configuration evaluation time because will happen during resolution - revisit this")
  @Test
  internal fun `Jenkins Pipeline Shared Groovy Libraries Plugin available in integrationTestImplementationConfiguration and HPI available in integrationTestRuntimeOnly`() {
    project.evaluate()

    val group = Condition<Dependency>(Predicate {
      it.group == "org.jenkins-ci.plugins.workflow"
    }, "org.jenkins-ci.plugins.workflow")
    val name = Condition<Dependency>(Predicate {
      it.name == "workflow-cps-global-lib"
    }, "workflow-cps-global-lib")
    val jar = Condition<Dependency>(Predicate {
      it is ExternalModuleDependency && it.artifacts.any { it.extension == "jar" }
    }, "jar extension")
    val hpi = Condition<Dependency>(Predicate {
      it is ExternalModuleDependency && it.artifacts.any { it.extension == "hpi" }
    }, "hpi extension")

    val implementation = project.configurations.getByName("integrationTestImplementation")
    assertThat(implementation.incoming.dependencies).haveExactly(1, allOf(group, name, jar))
    val runtimeOnly = project.configurations.getByName("integrationTestRuntimeOnly")
    assertThat(runtimeOnly.incoming.dependencies).haveExactly(1, allOf(group, name, hpi))
  }

  @Test
  internal fun `Jenkins WAR is available in Jenkins test runtime configuration`() {
    project.evaluate()

    val group = Condition<Dependency>(Predicate {
      it.group == "org.jenkins-ci.main"
    }, "org.jenkins-ci.main")
    val name = Condition<Dependency>(Predicate {
      it.name == "jenkins-war"
    }, "jenkins-war")
    val extension = Condition<Dependency>(Predicate {
      it is ExternalModuleDependency && it.artifacts.any { it.extension == "war" }
    }, "war")

    val configuration = project.configurations.getByName("jenkinsTestLibrariesRuntimeOnly")
    assertThat(configuration.incoming.dependencies).haveExactly(1, allOf(group, name, extension))
  }

  @Test
  internal fun `Jenkins Core dependency is available in Jenkins core library configuration`() {
    project.evaluate()

    val group = Condition<Dependency>(Predicate {
      it.group == "org.jenkins-ci.main"
    }, "org.jenkins-ci.main")
    val name = Condition<Dependency>(Predicate {
      it.name == "jenkins-core"
    }, "jenkins-core")

    val configuration = project.configurations.getByName("jenkinsCoreLibraries")
    assertThat(configuration.incoming.dependencies).haveExactly(1, allOf(group, name))
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
  internal fun `integrationTest task is in the verification group and has a description`() {
    val integrationTest = project.tasks.getByName("integrationTest")

    assertThat(integrationTest).isNotNull().isInstanceOf(org.gradle.api.tasks.testing.Test::class.java)
    assertThat(integrationTest.group).isEqualTo(JavaBasePlugin.VERIFICATION_GROUP)
    assertThat(integrationTest.description).isNotNull()
  }

  @Test
  internal fun `groovydocJar task is created`() {
    val groovydocJar = project.tasks.getByName("groovydocJar")
    assertThat(groovydocJar)
      .isNotNull()
      .isInstanceOf(Jar::class.java)
  }

  @Test
  internal fun `sourcesJar task is created`() {
    val groovydocJar = project.tasks.getByName("sourcesJar")
    assertThat(groovydocJar)
      .isNotNull()
      .isInstanceOf(Jar::class.java)
  }

  @Test
  internal fun `configuration exists for Jenkins plugins HPI and JPI dependencies`() {
    val config = project.configurations.getByName("jenkinsPluginHpisAndJpis")
    assertThat(config)
      .isNotNull
    assertThat(config.isVisible).isFalse()
  }

  @Test
  internal fun `configuration exists for Jenkins plugins JAR dependencies`() {
    val config = project.configurations.getByName("jenkinsPluginLibraries")
    assertThat(config)
      .isNotNull
    assertThat(config.isVisible).isFalse()
  }

  @Test
  internal fun `configuration exists for Jenkins core dependencies`() {
    val config = project.configurations.getByName("jenkinsCoreLibraries")
    assertThat(config)
      .isNotNull
    assertThat(config.isVisible).isFalse()
  }

  @Test
  internal fun `configuration exists for Jenkins test dependencies`() {
    val config = project.configurations.getByName("jenkinsTestLibraries")
    assertThat(config)
      .isNotNull
    assertThat(config.isVisible).isFalse()
  }

  @Test
  internal fun `integrationTestImplementation extends configurations for Jenkins core, plugins, and testing`() {
    val configuration = project.configurations.getByName("integrationTestImplementation")

    assertThat(configuration.extendsFrom.map { it.name })
      .isNotEmpty
      .contains("jenkinsPluginLibraries", "jenkinsCoreLibraries", "jenkinsTestLibraries")
  }

  @Test
  internal fun `integrationTestRuntimeOnly extends configurations for Jenkins test and plugin runtime`() {
    val configuration = project.configurations.getByName("integrationTestRuntimeOnly")

    assertThat(configuration.extendsFrom.map { it.name })
      .isNotEmpty
      .contains("jenkinsTestLibrariesRuntimeOnly", "jenkinsPluginHpisAndJpis")
  }

  @Test
  internal fun `code generation tasks do not have a group`() {
    val generationTasks = project.tasks.withType(GenerateJavaFile::class.java)

    assertThat(generationTasks)
      .isNotEmpty
      .allSatisfy {
        assertThat(it.group)
          .`as`("Group is not set")
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
