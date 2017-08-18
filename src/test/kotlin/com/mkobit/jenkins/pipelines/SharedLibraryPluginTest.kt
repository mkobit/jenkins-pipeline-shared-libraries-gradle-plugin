package com.mkobit.jenkins.pipelines

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Condition
import org.assertj.core.condition.AllOf.allOf
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.jvm.tasks.Jar
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
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
    assertThat(project.pluginManager).satisfies {
      assertThat(it.hasPlugin("groovy")).isTrue()
    }
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
      assertThat(mavenArtifactRepository.name).isEqualTo(SharedLibraryPlugin.JENKINS_REPOSITORY_NAME)
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
    assertThat(main.groovy.srcDirs).anySatisfy {
      assertThat(it.endsWith("src"))
    }
  }

  @Test
  internal fun `vars is a Groovy source directory`() {
    val convention = project.convention.getPlugin(JavaPluginConvention::class.java)
    val main = convention.sourceSets.getByName("main")
    assertThat(main).isNotNull()
    assertThat(main.groovy.srcDirs).anySatisfy {
      assertThat(it.endsWith("src"))
    }
  }

  @Test
  internal fun `src is a resources directory`() {
    val convention = project.convention.getPlugin(JavaPluginConvention::class.java)
    val main = convention.sourceSets.getByName("main")
    assertThat(main).isNotNull()
    assertThat(main.resources.srcDirs).anySatisfy {
      assertThat(it.endsWith("src"))
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
      it.version == "2.4.8"
    }, "2.4.8")
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

  @NotImplementedYet
  @Test
  internal fun `Jenkins Pipeline Unit is available in testImplementation configuration`() {
  }

  @Test
  internal fun `Jenkins Test Harness is available in integrationTestImplementation configuration`() {
    project.evaluate()

    val group = Condition<Dependency>(Predicate {
      it.group == "org.jenkins-ci.main"
    }, "org.jenkins-ci.main")
    val name = Condition<Dependency>(Predicate {
      it.name == "jenkins-test-harness"
    }, "jenkins-test-harness")

    val implementation = project.configurations.getByName("integrationTestImplementation")
    assertThat(implementation.incoming.dependencies).haveExactly(1, allOf(group, name))
  }

  @Test
  internal fun `Jenkins WAR is available in integrationTestRuntimeOnly configuration`() {
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

    val implementation = project.configurations.getByName("integrationTestRuntimeOnly")
    assertThat(implementation.incoming.dependencies).haveExactly(1, allOf(group, name, extension))
  }

  @Test
  internal fun `Jenkins Core dependency is available in integrationTestImplementation configuration`() {
    project.evaluate()

    val group = Condition<Dependency>(Predicate {
      it.group == "org.jenkins-ci.main"
    }, "org.jenkins-ci.main")
    val name = Condition<Dependency>(Predicate {
      it.name == "jenkins-core"
    }, "jenkins-core")

    val implementation = project.configurations.getByName("integrationTestImplementation")
    assertThat(implementation.incoming.dependencies).haveExactly(1, allOf(group, name))
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

  @NotImplementedYet
  @Test
  internal fun `Jenkins Global Library plugin implementation and HPI dependencies are added`() {
  }

  // Internal function needed here to trigger evaluation
  private fun Project.evaluate(): Unit {
    (this as ProjectInternal).evaluate()
  }
}
