package com.mkobit.jenkins.pipelines

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.inspectors.filterMatching
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import testsupport.gradle.TestedGradleVersion
import testsupport.gradle.withTestProject
import kotlin.io.path.writeText

/**
 * GradleRunner coverage for the peer-shared-library DSL in #158, grouped by the project
 * structure the consumer is using:
 *
 *  - **multi-project**: root + subproject(s) in one Gradle build, declared via `project(":peer")`.
 *  - **composite build**: root + `includeBuild`, declared via GAV that substitutes to the included
 *    build by group+name.
 *  - **binary GAV (deferred)**: `sharedLibrary("g:a:v")` from a Maven repo — blocked on the
 *    sources-JAR + `ArtifactTransform` follow-up.
 *
 * Each leaf describes asserts the same two contracts where applicable:
 *  1. `peerLibrarySource` resolves to the expected directories (variant attribute matching).
 *  2. The peer's compiled JAR ends up on `compileClasspath` (standard Java component variants).
 */
class SharedLibraryPluginPeerLibraryTest :
  DescribeSpec({
    describe("multi-project") {
      describe("simple project dep") {
        withData(TestedGradleVersion.filtered) { gradleVersion ->
          withTestProject {
            settingsFile.writeText(peerLibrarySettings("peer-multi-project", includes = listOf("peer-lib")))
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
        withData(TestedGradleVersion.filtered) { gradleVersion ->
          withTestProject {
            settingsFile.writeText(peerLibrarySettings("peer-transitive-root", includes = listOf("A", "B")))
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
        withData(TestedGradleVersion.filtered) { gradleVersion ->
          withTestProject {
            settingsFile.writeText(peerLibrarySettings("peer-cycle-root", includes = listOf("A", "B")))
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
        withData(TestedGradleVersion.filtered) { gradleVersion ->
          withTestProject {
            settingsFile.writeText(peerLibrarySettings("peer-overrides-root", includes = listOf("peer-lib")))
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
        withData(TestedGradleVersion.filtered) { gradleVersion ->
          withTestProject {
            settingsFile.writeText(peerLibrarySettings("peer-compile-root", includes = listOf("peer-lib")))
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

      describe("missing peer plugin: clear variant-selection error when project(\":lib\") does not apply the plugin") {
        withData(TestedGradleVersion.filtered) { gradleVersion ->
          withTestProject {
            settingsFile.writeText(peerLibrarySettings("peer-missing-plugin-root", includes = listOf("peer-lib")))
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
            // peer-lib applies the bare java-library plugin — it has no sharedLibrarySourceElements
            // variant. Consumer resolution must fail loudly, not silently produce zero peer sources.
            file("peer-lib/build.gradle.kts").writeText("plugins { `java-library` }")
            file("peer-lib/src/main/java/com/example/Placeholder.java").writeText(
              "package com.example; public class Placeholder {}",
            )

            val result = runner(gradleVersion).buildAndFail("printResolved")

            // Gradle's variant-selection failure names both the missing attribute value and the
            // configurations that were considered — the surface we care about for diagnosis.
            result.output shouldContain "jenkins-shared-library"
          }
        }
      }
    }

    describe("composite build") {
      describe("GAV substitution via includeBuild") {
        withData(TestedGradleVersion.filtered) { gradleVersion ->
          withTestProject {
            // Composite builds substitute external coordinates to the included build's projects by
            // matching group + name. Consumer declares a GAV; Gradle replaces it with the local
            // project. Project paths from an included build are not visible to the root.
            settingsFile.writeText(peerLibrarySettings("peer-composite-root", includeBuild = "included"))
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

            file("included/settings.gradle.kts").writeText(peerLibrarySettings("peer-lib"))
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

    describe("binary GAV (deferred — sources-JAR + ArtifactTransform follow-up)") {
      // Blocked: the sharedLibrarySourceElements variant ships a directory artefact, which
      // maven-publish's metadata pipeline cannot checksum / upload. Two paths forward:
      //  1. Add a sibling sources-JAR variant (zips the directory at publish time) + a
      //     consumer-side ArtifactTransform that unzips back to a directory.
      //  2. Route GAV consumers to the standard `-sources` classifier with the same transform.
      // Until either lands, attaching the directory variant with skip() keeps cross-project paths
      // green; this describe stays xdescribed as the unblock signal.
      xdescribe("sharedLibrary(\"group:artifact:version\") from a local Maven repo") {
        withData(TestedGradleVersion.filtered) { gradleVersion ->
          withTestProject {
            val mavenRepoPath = dir.resolve("local-maven-repo").toUri().toString()

            file("producer/settings.gradle.kts").writeText(peerLibrarySettings("peer-lib"))
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

            runner(gradleVersion)
              .withProjectDir(dir.resolve("producer").toFile())
              .build("publish")

            settingsFile.writeText(
              peerLibrarySettings("peer-binary-consumer", extraMavenRepo = mavenRepoPath),
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

// ── Test fixtures ────────────────────────────────────────────────────────────

private const val BASE_SETTINGS = """plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://repo.jenkins-ci.org/public/")"""

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

/**
 * Builds a settings.gradle.kts that wires the standard Jenkins + Maven Central repositories.
 *
 * @param includes project paths to `include(...)` (multi-project layout)
 * @param includeBuild relative path to an included build (composite layout)
 * @param extraMavenRepo URI of an additional Maven repository (binary-GAV scenarios)
 */
private fun peerLibrarySettings(
  rootName: String,
  includes: List<String> = emptyList(),
  includeBuild: String? = null,
  extraMavenRepo: String? = null,
): String =
  buildString {
    append(BASE_SETTINGS)
    if (extraMavenRepo != null) append("\n        maven(\"$extraMavenRepo\")")
    append("\n    }\n}\n")
    appendLine("rootProject.name = \"$rootName\"")
    if (includes.isNotEmpty()) appendLine("include(${includes.joinToString { "\"$it\"" }})")
    if (includeBuild != null) appendLine("includeBuild(\"$includeBuild\")")
  }

/** Standard root build.gradle.kts: applies the plugin, splices the supplied DSL body, appends the `printResolved` task. */
private fun rootBuildFile(sharedLibraryBody: String): String =
  """
  plugins { id("com.mkobit.jenkins.pipelines.shared-library") }
  ${sharedLibraryBody.trimIndent()}
  $PRINT_RESOLVED_TASK
  """.trimIndent()

/** Minimal peer subproject build.gradle.kts. Optionally declares own peer projects (for transitive / cycle setups). */
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

// ── GradleRunner shortcuts ───────────────────────────────────────────────────

private fun GradleRunner.build(vararg args: String): BuildResult = withArguments(*args).build()

private fun GradleRunner.buildAndFail(vararg args: String): BuildResult = withArguments(*args).buildAndFail()

// ── Output extractors / assertions ───────────────────────────────────────────

private fun BuildResult.peerSourceLines(): List<String> =
  output.lines().filterMatching { it.shouldStartWith("peer-source:") }

private fun BuildResult.compileLines(): List<String> =
  output.lines().filterMatching { it.shouldStartWith("compile:") }

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
