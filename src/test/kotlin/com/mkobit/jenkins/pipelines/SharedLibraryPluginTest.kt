package com.mkobit.jenkins.pipelines

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.types.shouldBeInstanceOf
import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.tasks.GroovySourceDirectorySet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.testfixtures.ProjectBuilder
import testsupport.kotest.shouldBePresent
import testsupport.kotest.shouldHaveValue

internal class SharedLibraryPluginTest :
  DescribeSpec({
    lateinit var project: Project

    beforeTest {
      project = ProjectBuilder.builder().build()
      project.pluginManager.apply("com.mkobit.jenkins.pipelines.shared-library")
      (project as ProjectInternal).evaluate()
    }

    it("applies the Groovy plugin") {
      project.pluginManager.hasPlugin("groovy") shouldBe true
    }

    describe("main source set") {
      it("includes src as a Groovy source directory") {
        val main = project.extensions.getByType(SourceSetContainer::class.java).getByName("main")
        main.extensions
          .getByType(GroovySourceDirectorySet::class.java)
          .srcDirs
          .map { it.name }
          .shouldContain("src")
      }

      it("includes vars as a Groovy source directory") {
        val main = project.extensions.getByType(SourceSetContainer::class.java).getByName("main")
        main.extensions
          .getByType(GroovySourceDirectorySet::class.java)
          .srcDirs
          .map { it.name }
          .shouldContain("vars")
      }

      it("has resources as the only resources directory") {
        val main = project.extensions.getByType(SourceSetContainer::class.java).getByName("main")
        main.resources.srcDirs shouldHaveSize 1
        main.resources.srcDirs
          .first()
          .name shouldBe "resources"
      }

      it("has no Java sources") {
        val main = project.extensions.getByType(SourceSetContainer::class.java).getByName("main")
        main.java.srcDirs.shouldBeEmpty()
      }
    }

    describe("configurations") {
      it("jenkinsPlugin is a user-facing declaration bucket") {
        val config = project.configurations.getByName("jenkinsPlugin")
        config.isCanBeResolved shouldBe false
        config.isCanBeConsumed shouldBe false
        config.description.shouldNotBeNull().shouldNotBeBlank()
      }

      it("compileOnly extends jenkinsPlugin") {
        project.configurations
          .getByName("compileOnly")
          .extendsFrom
          .map { it.name }
          .shouldContain("jenkinsPlugin")
      }

      it("testImplementation extends jenkinsPlugin") {
        project.configurations
          .getByName("testImplementation")
          .extendsFrom
          .map { it.name }
          .shouldContain("jenkinsPlugin")
      }

      it("integrationTestImplementation extends jenkinsPlugin") {
        project.configurations
          .getByName("integrationTestImplementation")
          .extendsFrom
          .map { it.name }
          .shouldContain("jenkinsPlugin")
      }

      describe("jenkinsPluginHpis") {
        it("is resolvable") {
          project.configurations
            .getByName("jenkinsPluginHpis")
            .isCanBeResolved
            .shouldBeTrue()
        }

        it("is not consumable") {
          project.configurations
            .getByName("jenkinsPluginHpis")
            .isCanBeConsumed
            .shouldBeFalse()
        }

        it("has a description") {
          project.configurations
            .getByName("jenkinsPluginHpis")
            .description
            .shouldNotBeNull()
            .shouldNotBeBlank()
        }

        it("requests hpi artifact type") {
          val attr =
            project.configurations
              .getByName("jenkinsPluginHpis")
              .attributes
              .getAttribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE)
          attr shouldBe "hpi"
        }

        it("requests hpi jenkins artifact attribute") {
          val attr =
            project.configurations
              .getByName("jenkinsPluginHpis")
              .attributes
              .getAttribute(JenkinsPluginRule.JENKINS_ARTIFACT_ATTRIBUTE)
          attr shouldBe "hpi"
        }

        it("extends jenkinsPlugin") {
          project.configurations
            .getByName("jenkinsPluginHpis")
            .extendsFrom
            .map { it.name }
            .shouldContain("jenkinsPlugin")
        }
      }

      describe("sharedLibraryIvy") {
        it("is not consumable") {
          project.configurations
            .getByName("sharedLibraryIvy")
            .isCanBeConsumed
            .shouldBeFalse()
        }
        it("has a description") {
          project.configurations
            .getByName("sharedLibraryIvy")
            .description
            .shouldNotBeNull()
            .shouldNotBeBlank()
        }
      }

      it("localLibraryRetrieverAnnotationProcessor has no annotation-indexer — index is generated by GenerateLocalLibraryFiles") {
        val deps = project.configurations.getByName("localLibraryRetrieverAnnotationProcessor").dependencies
        deps.any { it.group == "org.jenkins-ci" && it.name == "annotation-indexer" }.shouldBeFalse()
      }
    }

    describe("extension defaults") {
      it("autoRegisterLibrary defaults to true") {
        val ext = project.extensions.getByType(SharedLibraryExtension::class.java)
        ext.autoRegisterLibrary.shouldBePresent().shouldBeTrue()
      }

      it("implicit defaults to true") {
        val ext = project.extensions.getByType(SharedLibraryExtension::class.java)
        ext.implicit.shouldBePresent().shouldBeTrue()
      }

      it("libraryName defaults to project name") {
        val ext = project.extensions.getByType(SharedLibraryExtension::class.java)
        ext.libraryName shouldHaveValue project.name
      }

      it("maxParallelJenkinsTests defaults to 1") {
        val ext = project.extensions.getByType(SharedLibraryExtension::class.java)
        ext.maxParallelJenkinsTests shouldHaveValue 1
      }
    }

    describe("libraryName is reflected in test.library.0.name system property") {
      it("integrationTest injects libraryName as test.library.0.name") {
        val ext = project.extensions.getByType(SharedLibraryExtension::class.java)
        val task = project.tasks.getByName("integrationTest").shouldBeInstanceOf<Test>()
        val provider = task.jvmArgumentProviders.filterIsInstance<SharedLibrariesArgumentProvider>().single()
        provider.selfLibraryName shouldHaveValue ext.libraryName.shouldBePresent()
      }
    }

    describe("implicit is reflected in test.library.0.implicit system property") {
      it("integrationTest injects implicit=true by default") {
        val task = project.tasks.getByName("integrationTest").shouldBeInstanceOf<Test>()
        val provider = task.jvmArgumentProviders.filterIsInstance<SharedLibrariesArgumentProvider>().single()
        provider.selfLibraryImplicit shouldHaveValue true
      }
    }

    describe("attribute schema") {
      it("registers JENKINS_ARTIFACT_ATTRIBUTE disambiguation rule") {
        val schema = project.dependencies.attributesSchema
        val attr = JenkinsPluginRule.JENKINS_ARTIFACT_ATTRIBUTE
        schema.hasAttribute(attr).shouldBeTrue()
      }
    }

    describe("tasks") {
      it("integrationTest is in the verification group") {
        val task = project.tasks.getByName("integrationTest")
        task.shouldBeInstanceOf<Test>()
        task.group shouldBe JavaBasePlugin.VERIFICATION_GROUP
        task.description.shouldNotBeBlank()
      }

      it("integrationTest has maxParallelForks = 1") {
        val task = project.tasks.getByName("integrationTest").shouldBeInstanceOf<Test>()
        task.maxParallelForks shouldBe 1
      }

      it("integrationTest has maxHeapSize = 2g") {
        val task = project.tasks.getByName("integrationTest").shouldBeInstanceOf<Test>()
        task.maxHeapSize shouldBe "2g"
      }

      it("integrationTest injects test.library.0.location pointing at syncSharedLibrarySource output") {
        val task = project.tasks.getByName("integrationTest").shouldBeInstanceOf<Test>()
        val provider = task.jvmArgumentProviders.filterIsInstance<SharedLibrariesArgumentProvider>().single()
        provider.selfLibraryLocation.shouldBePresent {
          it.asFile shouldBe
            project.layout.buildDirectory
              .dir("sharedLibrarySource/${project.name}")
              .get()
              .asFile
        }
      }

      it("integrationTest injects test.library.0.name system property") {
        val task = project.tasks.getByName("integrationTest").shouldBeInstanceOf<Test>()
        val provider = task.jvmArgumentProviders.filterIsInstance<SharedLibrariesArgumentProvider>().single()
        provider.selfLibraryName shouldHaveValue project.name
      }

      it("generateLocalLibraryFiles task is registered") {
        project.tasks
          .getByName("generateLocalLibraryFiles")
          .shouldBeInstanceOf<GenerateLocalLibraryFiles>()
      }

      it("generateLocalLibraryFiles generateAutoRegistrar defaults to true") {
        val task = project.tasks.getByName("generateLocalLibraryFiles").shouldBeInstanceOf<GenerateLocalLibraryFiles>()
        task.generateAutoRegistrar shouldHaveValue true
      }

      it("groovydocJar is created with a description") {
        val task = project.tasks.getByName("groovydocJar")
        task.shouldBeInstanceOf<Jar>()
        task.description.shouldNotBeBlank()
      }

      it("sourcesJar is created with a description") {
        val task = project.tasks.getByName("sourcesJar")
        task.shouldBeInstanceOf<Jar>()
        task.description.shouldNotBeBlank()
      }

      it("registers jenkinsTestSuite build service with maxParallelUsages = 1") {
        val reg =
          project.gradle.sharedServices.registrations
            .findByName("jenkinsTestSuite")
        reg.shouldNotBeNull()
        reg.maxParallelUsages shouldHaveValue 1
      }
    }

    describe("JenkinsTestSuiteExtension") {
      fun suiteNamed(name: String): JvmTestSuite =
        project.extensions
          .getByType(org.gradle.testing.base.TestingExtension::class.java)
          .suites
          .getByName(name)
          .shouldBeInstanceOf()

      it("test suite has JenkinsTestSuiteExtension disabled by default") {
        suiteNamed("test").jenkins.useTestHarness shouldHaveValue false
      }

      it("integrationTest suite has extension with enabled = true") {
        suiteNamed("integrationTest").jenkins.useTestHarness shouldHaveValue true
      }

      it("withJenkins is idempotent — calling twice leaves enabled = true") {
        val suite = suiteNamed("integrationTest")
        val sharedLibrary = project.extensions.getByType(SharedLibraryExtension::class.java)
        @Suppress("DEPRECATION")
        sharedLibrary.withJenkins(suite)
        @Suppress("DEPRECATION")
        sharedLibrary.withJenkins(suite)
        suite.jenkins.useTestHarness shouldHaveValue true
      }

      it("integrationTest task has exactly one SharedLibrariesArgumentProvider after double withJenkins call") {
        val sharedLibrary = project.extensions.getByType(SharedLibraryExtension::class.java)
        val suite = suiteNamed("integrationTest")
        @Suppress("DEPRECATION")
        sharedLibrary.withJenkins(suite)
        project.tasks.getByName("integrationTest").shouldBeInstanceOf<Test> {
          it.jvmArgumentProviders.filterIsInstance<SharedLibrariesArgumentProvider>() shouldHaveSize 1
        }
      }
    }

    xit("Jenkins Global Library plugin implementation and HPI dependencies are added") {}
    xit("resources directory is available for GDSL support in IntelliJ") {}
  })
