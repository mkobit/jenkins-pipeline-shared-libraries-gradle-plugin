package com.mkobit.jenkins.pipelines

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome
import testsupport.TestProject
import testsupport.TestedGradleVersion
import testsupport.withTestProject
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

class SharedLibraryPluginSmokeTest :
  DescribeSpec({
    fun withSharedLibraryProject(block: TestProject.() -> Unit) =
      withTestProject {
        buildFile.writeText(
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
        block()
      }

    describe("plugin application") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withSharedLibraryProject {
          val result = runner(gradleVersion).withArguments("help").build()
          result.task(":help") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }
        }
      }
    }

    describe("expected tasks are registered") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withSharedLibraryProject {
          val result = runner(gradleVersion).withArguments("tasks", "--all").build()
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
        withSharedLibraryProject {
          val result =
            runner(gradleVersion)
              .withArguments("generateLocalLibraryFiles")
              .build()
          result.task(":generateLocalLibraryFiles") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }
          val testingDir =
            dir.resolve(
              "build/generated-src/localLibraryRetriever/java/com/mkobit/jenkins/pipelines/testing",
            )
          val retrieverFile = testingDir.resolve("LocalLibraryRetriever.java")
          retrieverFile.shouldExist()
          val source = retrieverFile.readText()
          source shouldContain "@Generated(\"com.mkobit.jenkins.pipelines.GenerateLocalLibraryFiles\")"
          source shouldContain "public static LibraryConfiguration implicitLibrary()"
          source shouldContain "public static LibraryConfiguration implicitLibrary(String name)"
          source shouldContain "test.library.name"
          source shouldContain "resources/**"
          dir
            .resolve("build/generated-src/localLibraryRetriever/resources/META-INF/hudson.remoting.ClassFilter")
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
        withTestProject {
          buildFile.writeText(
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
            runner(gradleVersion)
              .withArguments("generateLocalLibraryFiles")
              .build()
          result.task(":generateLocalLibraryFiles") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }
          dir
            .resolve(
              "build/generated-src/integrationTest/java/com/mkobit/jenkins/pipelines/testing/SharedLibraryAutoRegistrar.java",
            ).exists() shouldBe false
        }
      }
    }

    describe("compileIntegrationTestJava depends on generateLocalLibraryFiles") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withSharedLibraryProject {
          val result =
            runner(gradleVersion)
              .withArguments("compileIntegrationTestJava", "--dry-run")
              .build()
          result.output shouldContain ":generateLocalLibraryFiles"
          result.output shouldContain ":compileIntegrationTestJava"
        }
      }
    }

    describe("check lifecycle includes integrationTest") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withSharedLibraryProject {
          val result = runner(gradleVersion).withArguments("check", "--dry-run").build()
          result.output shouldContain ":integrationTest"
          result.output shouldContain ":test"
        }
      }
    }

    describe("sharedLibrary.plugins.plugin registers a dependency on the jenkinsPlugin configuration") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withTestProject {
          buildFile.writeText(
            """
            plugins {
                id("com.mkobit.jenkins.pipelines.shared-library")
            }
            sharedLibrary {
                plugins {
                    plugin("org.example:fake:1.0")
                }
            }
            """.trimIndent(),
          )
          val result = runner(gradleVersion).withArguments("help").build()
          result.task(":help") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }
        }
      }
    }

    describe("integrationTest sets buildDirectory system property to build dir for WarExploder") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withSharedLibraryProject {
          val result =
            runner(gradleVersion)
              .withArguments("printIntegrationTestWarExploderConfig")
              .build()
          val expectedBuildDir =
            dir
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
        withTestProject {
          settingsFile.writeText(
            """
            rootProject.name = "monorepo-root"
            include(":lib")
            """.trimIndent(),
          )
          buildFile.writeText("")
          file("lib/build.gradle.kts").writeText(
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
            runner(gradleVersion)
              .withArguments(":lib:printLibraryRoot")
              .build()
          val expectedRoot =
            dir
              .toRealPath()
              .resolve("lib")
              .toString()
          result.output shouldContain "root=$expectedRoot"
        }
      }
    }

    describe("integrationTest jvmArgumentProviders include JenkinsWarJvmArgumentProvider") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withTestProject {
          buildFile.writeText(
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
            runner(gradleVersion)
              .withArguments("printWarArgumentProvider")
              .build()
          result.output shouldContain "hasWarProvider=true"
        }
      }
    }
  })
