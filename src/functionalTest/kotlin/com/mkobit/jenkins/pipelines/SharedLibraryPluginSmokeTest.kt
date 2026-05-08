package com.mkobit.jenkins.pipelines

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome
import testsupport.TestedGradleVersion
import testsupport.withTestProject
import kotlin.io.path.readText
import kotlin.io.path.writeText

class SharedLibraryPluginSmokeTest :
  DescribeSpec({
    fun withSharedLibraryProject(block: (testsupport.TestProject) -> Unit) =
      withTestProject { project ->
        project.buildFile.writeText(
          """
          plugins {
              id("com.mkobit.jenkins.pipelines.shared-library")
          }
          tasks.register("printIntegrationTestWarExploderConfig") {
              val t = tasks.integrationTest
              doLast {
                  val testTask = t.get()
                  val buildDirProvider = testTask.jvmArgumentProviders
                      .filterIsInstance<com.mkobit.jenkins.pipelines.BuildDirJvmArgumentProvider>()
                      .firstOrNull()
                  println("buildDirectory=" + buildDirProvider?.dir?.get()?.asFile?.absolutePath)
                  println("outputDirs=" + testTask.outputs.files.joinToString(",") { it.absolutePath })
              }
          }
          """.trimIndent(),
        )
        block(project)
      }

    describe("plugin application") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withSharedLibraryProject { project ->
          val result = project.runner(gradleVersion).withArguments("help").build()
          result.task(":help")!!.outcome shouldBe TaskOutcome.SUCCESS
        }
      }
    }

    describe("expected tasks are registered") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withSharedLibraryProject { project ->
          val result = project.runner(gradleVersion).withArguments("tasks", "--all").build()
          result.output shouldContain "integrationTest"
          result.output shouldContain "generateLocalLibraryFiles"
          result.output shouldContain "groovydocJar"
          result.output shouldContain "sourcesJar"
          result.output shouldContain "groovydoc"
          result.output shouldContain "compileGroovy"
        }
      }
    }

    describe("generateLocalLibraryFiles produces LocalLibraryRetriever.java and ClassFilter resource") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withSharedLibraryProject { project ->
          val result =
            project
              .runner(gradleVersion)
              .withArguments("generateLocalLibraryFiles")
              .build()
          result.task(":generateLocalLibraryFiles")!!.outcome shouldBe TaskOutcome.SUCCESS
          val testingDir =
            project.dir.resolve(
              "build/generated-src/integrationTest/java/com/mkobit/jenkins/pipelines/testing",
            )
          val retrieverFile = testingDir.resolve("LocalLibraryRetriever.java")
          retrieverFile.shouldExist()
          val source = retrieverFile.readText()
          source shouldContain "@Generated(\"com.mkobit.jenkins.pipelines.GenerateLocalLibraryFiles\")"
          source shouldContain "public static LibraryConfiguration implicitLibrary()"
          source shouldContain "public static LibraryConfiguration implicitLibrary(String name)"
          source shouldContain "test.library.name"
          source shouldContain "resources/**"
          project.dir
            .resolve("build/generated-src/integrationTest/resources/META-INF/hudson.remoting.ClassFilter")
            .shouldExist()

          val autoRegistrarFile = testingDir.resolve("SharedLibraryAutoRegistrar.java")
          autoRegistrarFile.shouldExist()
          val autoRegistrarSource = autoRegistrarFile.readText()
          autoRegistrarSource shouldContain "@Generated(\"com.mkobit.jenkins.pipelines.GenerateLocalLibraryFiles\")"
          autoRegistrarSource shouldContain "@Initializer(after = InitMilestone.EXTENSIONS_AUGMENTED)"
          autoRegistrarSource shouldContain "public static void registerLibrary()"
          autoRegistrarSource shouldContain "test.library.auto.register"
          autoRegistrarSource shouldContain "test.library.name"
        }
      }
    }

    describe("generateLocalLibraryFiles skips SharedLibraryAutoRegistrar when autoRegisterLibrary = false") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withTestProject { project ->
          project.buildFile.writeText(
            """
            plugins {
                id("com.mkobit.jenkins.pipelines.shared-library")
            }
            sharedLibrary {
                autoRegisterLibrary = false
            }
            """.trimIndent(),
          )
          val result =
            project
              .runner(gradleVersion)
              .withArguments("generateLocalLibraryFiles")
              .build()
          result.task(":generateLocalLibraryFiles")!!.outcome shouldBe TaskOutcome.SUCCESS
          project.dir
            .resolve(
              "build/generated-src/integrationTest/java/com/mkobit/jenkins/pipelines/testing/SharedLibraryAutoRegistrar.java",
            ).toFile()
            .exists() shouldBe false
        }
      }
    }

    describe("compileIntegrationTestJava depends on generateLocalLibraryFiles") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withSharedLibraryProject { project ->
          val result =
            project
              .runner(gradleVersion)
              .withArguments("compileIntegrationTestJava", "--dry-run")
              .build()
          result.output shouldContain ":generateLocalLibraryFiles"
          result.output shouldContain ":compileIntegrationTestJava"
        }
      }
    }

    describe("check lifecycle includes integrationTest") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withSharedLibraryProject { project ->
          val result = project.runner(gradleVersion).withArguments("check", "--dry-run").build()
          result.output shouldContain ":integrationTest"
          result.output shouldContain ":test"
        }
      }
    }

    describe("jenkinsPlugin configuration accepts a dependency declaration") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withTestProject { project ->
          project.buildFile.writeText(
            """
            plugins {
                id("com.mkobit.jenkins.pipelines.shared-library")
            }
            dependencies {
                jenkinsPlugin("org.example:fake:1.0")
            }
            """.trimIndent(),
          )
          val result = project.runner(gradleVersion).withArguments("help").build()
          result.task(":help")!!.outcome shouldBe TaskOutcome.SUCCESS
        }
      }
    }

    describe("jenkinsPlugin configuration accepts a platform BOM") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withTestProject { project ->
          project.buildFile.writeText(
            """
            plugins {
                id("com.mkobit.jenkins.pipelines.shared-library")
            }
            dependencies {
                jenkinsPlugin(platform("org.example:fake-bom:1.0"))
            }
            """.trimIndent(),
          )
          val result = project.runner(gradleVersion).withArguments("help").build()
          result.task(":help")!!.outcome shouldBe TaskOutcome.SUCCESS
        }
      }
    }

    describe("integrationTest sets buildDirectory system property to build dir for WarExploder") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withSharedLibraryProject { project ->
          val result =
            project
              .runner(gradleVersion)
              .withArguments("printIntegrationTestWarExploderConfig")
              .build()
          val expectedBuildDir =
            project.dir
              .toRealPath()
              .resolve("build")
              .toString()
          result.output shouldContain "buildDirectory=$expectedBuildDir"
          result.output shouldContain "jenkins-for-test"
        }
      }
    }

    describe("monorepo: test.library.root resolves relative to subproject projectDir, not rootDir") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withTestProject { project ->
          project.settingsFile.writeText(
            """
            rootProject.name = "monorepo-root"
            include(":lib")
            """.trimIndent(),
          )
          project.buildFile.writeText("")
          project.file("lib/build.gradle.kts").writeText(
            """
            plugins {
                id("com.mkobit.jenkins.pipelines.shared-library")
            }
            tasks.register("printLibraryRoot") {
                val t = tasks.integrationTest
                doLast {
                    val testTask = t.get()
                    println("root=" + testTask.systemProperties["test.library.root"])
                }
            }
            """.trimIndent(),
          )
          val result =
            project
              .runner(gradleVersion)
              .withArguments(":lib:printLibraryRoot")
              .build()
          val expectedRoot =
            project.dir
              .toRealPath()
              .resolve("lib")
              .toString()
          result.output shouldContain "root=$expectedRoot"
        }
      }
    }

    describe("integrationTest jvmArgumentProviders include JenkinsWarJvmArgumentProvider") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withTestProject { project ->
          project.buildFile.writeText(
            """
            plugins {
                id("com.mkobit.jenkins.pipelines.shared-library")
            }
            tasks.register("printWarArgumentProvider") {
                val t = tasks.integrationTest
                doLast {
                    val testTask = t.get()
                    val hasProvider = testTask.jvmArgumentProviders
                        .any { it is com.mkobit.jenkins.pipelines.JenkinsWarJvmArgumentProvider }
                    println("hasWarProvider=${'$'}hasProvider")
                }
            }
            """.trimIndent(),
          )
          val result =
            project
              .runner(gradleVersion)
              .withArguments("printWarArgumentProvider")
              .build()
          result.output shouldContain "hasWarProvider=true"
        }
      }
    }
  })
