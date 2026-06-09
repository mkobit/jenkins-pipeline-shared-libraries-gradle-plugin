package com.mkobit.jenkins.pipelines

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.inspectors.filterMatching
import io.kotest.inspectors.forOne
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import testsupport.gradle.build
import testsupport.gradle.buildAndFail
import testsupport.gradle.forGradleVersions
import testsupport.gradle.withTestProject
import testsupport.jenkins.jenkinsSettings
import kotlin.io.path.writeText

class SharedLibraryPluginPeerLibraryTest :
  DescribeSpec({
    describe("multi-project") {
      describe("simple project dep") {
        forGradleVersions { gradleVersion ->
          withTestProject {
            settingsFile.writeText(jenkinsSettings("peer-multi-project", includes = listOf("peer-lib")))
            buildFile.writeText(
              rootBuildFile(
                """
                sharedLibrary {
                    dependencies {
                        sharedLibrary(project(":peer-lib"))
                    }
                }
                """,
              ),
            )
            file("peer-lib/build.gradle.kts").writeText(barePeerSubproject())
            file("peer-lib/vars/peerStep.groovy").writeText("def call() {}")

            val result = runner(gradleVersion).build("printResolved")

            val sourceLines = result.peerSourceLines()
            sourceLines shouldHaveSize 1
            sourceLines.single() shouldContain "peer-lib"
            sourceLines.single() shouldContain "project :peer-lib"
            result.compileLines().forAtLeastOnePeer()
          }
        }
      }

      describe("transitive: root → A → B propagates B's source dir into root's peerLibrarySource") {
        forGradleVersions { gradleVersion ->
          withTestProject {
            settingsFile.writeText(jenkinsSettings("peer-transitive-root", includes = listOf("A", "B")))
            buildFile.writeText(
              rootBuildFile(
                """
                sharedLibrary {
                    dependencies {
                        sharedLibrary(project(":A"))
                    }
                }
                """,
              ),
            )
            file("A/build.gradle.kts").writeText(barePeerSubproject(declaresPeerProjects = listOf(":B")))
            file("A/vars/aStep.groovy").writeText("def call() {}")
            file("B/build.gradle.kts").writeText(barePeerSubproject())
            file("B/vars/bStep.groovy").writeText("def call() {}")

            val sourceLines = runner(gradleVersion).build("printResolved").peerSourceLines()
            sourceLines shouldHaveSize 2
            sourceLines.shouldContainProject(":A")
            sourceLines.shouldContainProject(":B")
          }
        }
      }

      describe("cycle: A ↔ B both declaring each other resolves safely with dedup") {
        forGradleVersions { gradleVersion ->
          withTestProject {
            settingsFile.writeText(jenkinsSettings("peer-cycle-root", includes = listOf("A", "B")))
            buildFile.writeText(
              rootBuildFile(
                """
                sharedLibrary {
                    dependencies {
                        sharedLibrary(project(":A"))
                    }
                }
                """,
              ),
            )
            file("A/build.gradle.kts").writeText(barePeerSubproject(declaresPeerProjects = listOf(":B")))
            file("A/vars/aStep.groovy").writeText("def call() {}")
            file("B/build.gradle.kts").writeText(barePeerSubproject(declaresPeerProjects = listOf(":A")))
            file("B/vars/bStep.groovy").writeText("def call() {}")

            // Gradle's dependency graph deduplicates: A and B appear once each, no infinite expansion.
            val sourceLines = runner(gradleVersion).build("printResolved").peerSourceLines()
            sourceLines shouldHaveSize 2
            sourceLines.shouldContainProject(":A")
            sourceLines.shouldContainProject(":B")
          }
        }
      }

      describe("DSL overrides: libraryName and implicit captured on the PeerLibrarySpec") {
        forGradleVersions { gradleVersion ->
          withTestProject {
            settingsFile.writeText(jenkinsSettings("peer-overrides-root", includes = listOf("peer-lib")))
            buildFile.writeText(
              """
              plugins { id("com.mkobit.jenkins.pipelines.shared-library") }
              sharedLibrary {
                  dependencies {
                      sharedLibrary(project(":peer-lib")) {
                          libraryName.set("renamed-in-tests")
                          implicit.set(false)
                      }
                  }
              }
              tasks.register("printSpecs") {
                  doLast {
                      sharedLibrary.dependencies.specs.get().forEach {
                          println("spec:" + it.identifier.get() + "|" + it.libraryName.get() + "|" + it.implicit.get())
                      }
                  }
              }
              """.trimIndent(),
            )
            file("peer-lib/build.gradle.kts").writeText(barePeerSubproject())
            file("peer-lib/vars/peerStep.groovy").writeText("def call() {}")

            val specLines =
              runner(gradleVersion)
                .build("printSpecs")
                .output
                .lines()
                .filterMatching { it.shouldStartWith("spec:") }
            specLines shouldHaveSize 1
            specLines.single() shouldContain ":peer-lib|renamed-in-tests|false"
          }
        }
      }

      describe("consumer src compiles against peer src symbols on compileOnly") {
        forGradleVersions { gradleVersion ->
          withTestProject {
            settingsFile.writeText(jenkinsSettings("peer-compile-root", includes = listOf("peer-lib")))
            buildFile.writeText(
              """
              plugins { id("com.mkobit.jenkins.pipelines.shared-library") }
              sharedLibrary {
                  dependencies {
                      sharedLibrary(project(":peer-lib"))
                  }
              }
              """.trimIndent(),
            )
            file("src/com/example/consumer/Consumer.groovy").writeText(
              """
              package com.example.consumer
              import com.example.peer.PeerType
              class Consumer {
                  String relay() { new PeerType().value() }
              }
              """.trimIndent(),
            )
            file("peer-lib/build.gradle.kts").writeText(barePeerSubproject())
            file("peer-lib/src/com/example/peer/PeerType.groovy").writeText(
              """
              package com.example.peer
              class PeerType {
                  String value() { "peer-value" }
              }
              """.trimIndent(),
            )

            val result = runner(gradleVersion).build("compileGroovy")
            result.task(":compileGroovy") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }
          }
        }
      }

      describe("peer library JVM args injected into integrationTest task") {
        forGradleVersions { gradleVersion ->
          withTestProject {
            settingsFile.writeText(jenkinsSettings("peer-jvm-args", includes = listOf("peer-lib")))
            buildFile.writeText(
              """
              plugins { id("com.mkobit.jenkins.pipelines.shared-library") }
              sharedLibrary {
                  dependencies {
                      sharedLibrary(project(":peer-lib"))
                  }
              }
              tasks.register("printPeerJvmArgs") {
                  dependsOn(":peer-lib:syncSharedLibrarySource")
                  doLast {
                      tasks.named<Test>("integrationTest").get()
                          .jvmArgumentProviders
                          .flatMap { it.asArguments() }
                          .filter { it.startsWith("-Dtest.library.") }
                          .forEach { println("peer-arg:" + it) }
                  }
              }
              """.trimIndent(),
            )
            file("peer-lib/build.gradle.kts").writeText(barePeerSubproject())
            file("peer-lib/vars/peerStep.groovy").writeText("def call() {}")

            val output = runner(gradleVersion).build("printPeerJvmArgs").output
            val argLines = output.lines().filter { it.startsWith("peer-arg:") }
            // index 0 = self-library, index 1 = declared peer — 3 properties each
            argLines shouldHaveSize 6
            argLines.forOne { it shouldContain "-Dtest.library.0.name=peer-jvm-args" }
            argLines.forOne { it shouldContain "-Dtest.library.0.location=" }
            argLines.forOne { it shouldContain "-Dtest.library.0.implicit=true" }
            argLines.forOne { it shouldContain "-Dtest.library.1.name=peer-lib" }
            argLines.forOne { it shouldContain "-Dtest.library.1.implicit=true" }
            argLines.forOne {
              it shouldContain "-Dtest.library.1.location="
              it shouldContain "peer-lib"
            }
          }
        }
      }

      describe("missing peer plugin: clear variant-selection error when project(\":lib\") does not apply the plugin") {
        forGradleVersions { gradleVersion ->
          withTestProject {
            settingsFile.writeText(jenkinsSettings("peer-missing-plugin-root", includes = listOf("peer-lib")))
            buildFile.writeText(
              rootBuildFile(
                """
                sharedLibrary {
                    dependencies {
                        sharedLibrary(project(":peer-lib"))
                    }
                }
                """,
              ),
            )
            // peer-lib applies bare java-library — no sharedLibrarySourceElements variant
            file("peer-lib/build.gradle.kts").writeText("plugins { `java-library` }")
            file("peer-lib/src/main/java/com/example/Placeholder.java").writeText(
              "package com.example; public class Placeholder {}",
            )

            val result = runner(gradleVersion).buildAndFail("printResolved")

            result.output shouldContain "jenkins-shared-library"
          }
        }
      }
    }

    describe("composite build") {
      describe("GAV substitution via includeBuild") {
        forGradleVersions { gradleVersion ->
          withTestProject {
            // GAV notation works because Gradle substitutes included-build projects by group:name match.
            settingsFile.writeText(jenkinsSettings("peer-composite-root", includeBuild = "included"))
            buildFile.writeText(
              rootBuildFile(
                """
                sharedLibrary {
                    dependencies {
                        sharedLibrary("com.example.composite:peer-lib:0.1.0")
                    }
                }
                """,
              ),
            )

            file("included/settings.gradle.kts").writeText(jenkinsSettings("peer-lib"))
            file("included/build.gradle.kts").writeText(
              """
              plugins { id("com.mkobit.jenkins.pipelines.shared-library") }
              group = "com.example.composite"
              version = "0.1.0"
              """.trimIndent(),
            )
            file("included/vars/peerStep.groovy").writeText("def call() {}")

            val result = runner(gradleVersion).build("printResolved")
            val sourceLines = result.peerSourceLines()
            sourceLines shouldHaveSize 1
            sourceLines.single() shouldContain "peer-lib"
            result.compileLines().forAtLeastOnePeer()
          }
        }
      }
    }

    // Tracked in https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/165
    // sharedLibrarySourceElements ships a directory artifact that Maven's publishing pipeline cannot
    // checksum or upload, so the variant metadata never lands in a repo. Unblocking this requires a
    // publish-side sources-JAR variant and a consumer-side ArtifactTransform to unzip it back to a
    // directory — a non-trivial design given that JSL source is not compiled before publishing.
    xdescribe("binary GAV (blocked on sources-JAR + ArtifactTransform — see issue #165)") {
      xdescribe("sharedLibrary(\"group:artifact:version\") from a local Maven repo") {
        forGradleVersions { gradleVersion ->
          withTestProject {
            val mavenRepoPath = dir.resolve("local-maven-repo").toUri().toString()

            file("producer/settings.gradle.kts").writeText(jenkinsSettings("peer-lib"))
            file("producer/build.gradle.kts").writeText(
              """
              plugins {
                  id("com.mkobit.jenkins.pipelines.shared-library")
                  `maven-publish`
              }
              group = "com.example.shared"
              version = "1.2.3"
              publishing {
                  publications {
                      create<MavenPublication>("mavenJava") { from(components["java"]) }
                  }
                  repositories { maven { url = uri("$mavenRepoPath") } }
              }
              """.trimIndent(),
            )
            file("producer/vars/peerStep.groovy").writeText("def call() {}")

            GradleRunner
              .create()
              .withProjectDir(dir.resolve("producer").toFile())
              .withGradleVersion(gradleVersion.version)
              .withPluginClasspath()
              .build("publish")

            settingsFile.writeText(
              jenkinsSettings("peer-binary-consumer") + "\n" +
                """
                dependencyResolutionManagement.repositories.maven { url = uri("$mavenRepoPath") }
                """.trimIndent(),
            )
            buildFile.writeText(
              rootBuildFile(
                """
                sharedLibrary {
                    dependencies {
                        sharedLibrary("com.example.shared:peer-lib:1.2.3")
                    }
                }
                """,
              ),
            )

            val result = runner(gradleVersion).build("printResolved")
            val sourceLines = result.peerSourceLines()
            sourceLines shouldHaveSize 1
            sourceLines.single() shouldContain "com.example.shared:peer-lib"
            result.compileLines().forAtLeastOnePeer()
          }
        }
      }
    }
  })

private const val PRINT_RESOLVED_TASK = """tasks.register("printResolved") {
    doLast {
        configurations.getByName("peerLibrarySource").incoming.artifacts.artifacts.forEach {
            println("peer-source:" + it.file.name + "|" + it.id.componentIdentifier)
        }
        configurations.getByName("compileClasspath").resolvedConfiguration.resolvedArtifacts.forEach {
            println("compile:" + it.file.name)
        }
    }
}"""

private fun rootBuildFile(sharedLibraryBody: String): String =
  """
  plugins { id("com.mkobit.jenkins.pipelines.shared-library") }
  ${sharedLibraryBody.trimIndent()}
  $PRINT_RESOLVED_TASK
  """.trimIndent()

private fun barePeerSubproject(declaresPeerProjects: List<String> = emptyList()): String =
  buildString {
    appendLine("""plugins { id("com.mkobit.jenkins.pipelines.shared-library") }""")
    if (declaresPeerProjects.isNotEmpty()) {
      appendLine("sharedLibrary {")
      appendLine("    dependencies {")
      declaresPeerProjects.forEach { appendLine("        sharedLibrary(project(\"$it\"))") }
      appendLine("    }")
      appendLine("}")
    }
  }

private fun BuildResult.peerSourceLines(): List<String> = output.lines().filterMatching { it.shouldStartWith("peer-source:") }

private fun BuildResult.compileLines(): List<String> = output.lines().filterMatching { it.shouldStartWith("compile:") }

private fun List<String>.forAtLeastOnePeer() {
  shouldNotBeEmpty()
  if (none { it.contains("peer-lib") }) {
    throw AssertionError("expected at least one compile: line to reference peer-lib, got:\n${joinToString("\n")}")
  }
}

private fun List<String>.shouldContainProject(projectPath: String) {
  if (none { it.contains("project $projectPath") }) {
    throw AssertionError("expected at least one line to reference project $projectPath, got:\n${joinToString("\n")}")
  }
}
