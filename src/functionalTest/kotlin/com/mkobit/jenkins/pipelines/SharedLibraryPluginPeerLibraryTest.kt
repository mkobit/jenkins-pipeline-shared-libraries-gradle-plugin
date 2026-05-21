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
import org.gradle.testkit.runner.TaskOutcome
import testsupport.gradle.TestedGradleVersion
import testsupport.gradle.withTestProject
import kotlin.io.path.writeText

/**
 * GradleRunner-driven coverage for the three peer-shared-library dependency forms in #158:
 *  - `sharedLibrary(project(":peer"))` in a multi-project build
 *  - `sharedLibrary(project(":peer"))` across a composite build (`includeBuild`)
 *  - `sharedLibrary("group:artifact:version")` via Gradle Module Metadata from a local Maven repo
 *
 * Each scenario verifies the same two contracts:
 *  1. `peerLibrarySource` resolves to the peer's source directory (variant attribute matching).
 *  2. The peer's compiled JAR ends up on `compileClasspath` (standard Java component variants).
 */
class SharedLibraryPluginPeerLibraryTest :
  DescribeSpec({
    val printResolvedTask =
      """
      tasks.register("printResolved") {
          doLast {
              configurations.getByName("peerLibrarySource").incoming.artifacts.artifacts.forEach {
                  println("peer-source:" + it.file.name + "|" + it.id.componentIdentifier)
              }
              configurations.getByName("compileClasspath").resolvedConfiguration.resolvedArtifacts.forEach {
                  println("compile:" + it.file.name)
              }
          }
      }
      """.trimIndent()

    val baseSettings =
      """
      plugins {
          id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
      }
      dependencyResolutionManagement {
          repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
          repositories {
              mavenCentral()
              maven("https://repo.jenkins-ci.org/public/")
          }
      }
      """.trimIndent()

    describe("multi-project: sharedLibrary(project(\":peer-lib\"))") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withTestProject {
          settingsFile.writeText(
            """
            $baseSettings
            rootProject.name = "peer-multi-project"
            include("peer-lib")
            """.trimIndent(),
          )
          buildFile.writeText(
            """
            plugins { id("com.mkobit.jenkins.pipelines.shared-library") }
            sharedLibrary {
                dependencies {
                    sharedLibrary(project(":peer-lib"))
                }
            }
            $printResolvedTask
            """.trimIndent(),
          )
          file("peer-lib/build.gradle.kts").writeText(
            """
            plugins { id("com.mkobit.jenkins.pipelines.shared-library") }
            """.trimIndent(),
          )
          file("peer-lib/vars/peerStep.groovy").writeText("def call() {}")

          val result = runner(gradleVersion).withArguments("printResolved").build()

          val sourceLines = result.output.lines().filterMatching { it.shouldStartWith("peer-source:") }
          sourceLines shouldHaveSize 1
          sourceLines.single() shouldContain "peer-lib"
          sourceLines.single() shouldContain "project :peer-lib"

          val compileLines = result.output.lines().filterMatching { it.shouldStartWith("compile:") }
          compileLines.forAtLeastOnePeer()
        }
      }
    }

    describe("composite build: sharedLibrary(\"group:artifact\") with includeBuild substitution") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withTestProject {
          // Composite builds substitute external coordinates to the included build's projects by
          // matching group + name. The consumer declares a GAV; Gradle replaces it with the local
          // project. Project paths from the included build are not directly visible to the root.
          settingsFile.writeText(
            """
            $baseSettings
            rootProject.name = "peer-composite-root"
            includeBuild("included")
            """.trimIndent(),
          )
          buildFile.writeText(
            """
            plugins { id("com.mkobit.jenkins.pipelines.shared-library") }
            sharedLibrary {
                dependencies {
                    sharedLibrary("com.example.composite:peer-lib:0.1.0")
                }
            }
            $printResolvedTask
            """.trimIndent(),
          )

          file("included/settings.gradle.kts").writeText(
            """
            $baseSettings
            rootProject.name = "peer-lib"
            """.trimIndent(),
          )
          file("included/build.gradle.kts").writeText(
            """
            plugins { id("com.mkobit.jenkins.pipelines.shared-library") }
            group = "com.example.composite"
            version = "0.1.0"
            """.trimIndent(),
          )
          file("included/vars/peerStep.groovy").writeText("def call() {}")

          val result = runner(gradleVersion).withArguments("printResolved").build()

          val sourceLines = result.output.lines().filterMatching { it.shouldStartWith("peer-source:") }
          sourceLines shouldHaveSize 1
          sourceLines.single() shouldContain "peer-lib"

          val compileLines = result.output.lines().filterMatching { it.shouldStartWith("compile:") }
          compileLines.forAtLeastOnePeer()
        }
      }
    }

    describe("transitive: root → A → B propagates B into root's peerLibrarySource") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withTestProject {
          settingsFile.writeText(
            """
            $baseSettings
            rootProject.name = "peer-transitive-root"
            include("A", "B")
            """.trimIndent(),
          )
          buildFile.writeText(
            """
            plugins { id("com.mkobit.jenkins.pipelines.shared-library") }
            sharedLibrary {
                dependencies {
                    sharedLibrary(project(":A"))
                }
            }
            $printResolvedTask
            """.trimIndent(),
          )
          file("A/build.gradle.kts").writeText(
            """
            plugins { id("com.mkobit.jenkins.pipelines.shared-library") }
            sharedLibrary {
                dependencies {
                    sharedLibrary(project(":B"))
                }
            }
            """.trimIndent(),
          )
          file("A/vars/aStep.groovy").writeText("def call() {}")
          file("B/build.gradle.kts").writeText(
            """
            plugins { id("com.mkobit.jenkins.pipelines.shared-library") }
            """.trimIndent(),
          )
          file("B/vars/bStep.groovy").writeText("def call() {}")

          val result = runner(gradleVersion).withArguments("printResolved").build()

          val sourceLines = result.output.lines().filterMatching { it.shouldStartWith("peer-source:") }
          sourceLines shouldHaveSize 2
          sourceLines.shouldContainProject(":A")
          sourceLines.shouldContainProject(":B")
        }
      }
    }

    describe("cycle: A ↔ B both declaring each other resolves safely (no stack overflow, deduped)") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withTestProject {
          settingsFile.writeText(
            """
            $baseSettings
            rootProject.name = "peer-cycle-root"
            include("A", "B")
            """.trimIndent(),
          )
          buildFile.writeText(
            """
            plugins { id("com.mkobit.jenkins.pipelines.shared-library") }
            sharedLibrary {
                dependencies {
                    sharedLibrary(project(":A"))
                }
            }
            $printResolvedTask
            """.trimIndent(),
          )
          file("A/build.gradle.kts").writeText(
            """
            plugins { id("com.mkobit.jenkins.pipelines.shared-library") }
            sharedLibrary {
                dependencies {
                    sharedLibrary(project(":B"))
                }
            }
            """.trimIndent(),
          )
          file("A/vars/aStep.groovy").writeText("def call() {}")
          file("B/build.gradle.kts").writeText(
            """
            plugins { id("com.mkobit.jenkins.pipelines.shared-library") }
            sharedLibrary {
                dependencies {
                    sharedLibrary(project(":A"))
                }
            }
            """.trimIndent(),
          )
          file("B/vars/bStep.groovy").writeText("def call() {}")

          val result = runner(gradleVersion).withArguments("printResolved").build()

          // Gradle's dependency graph deduplicates: A and B appear once each, no infinite expansion.
          val sourceLines = result.output.lines().filterMatching { it.shouldStartWith("peer-source:") }
          sourceLines shouldHaveSize 2
          sourceLines.shouldContainProject(":A")
          sourceLines.shouldContainProject(":B")
        }
      }
    }

    describe("DSL overrides: libraryName and implicit captured on the PeerLibrarySpec") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withTestProject {
          settingsFile.writeText(
            """
            $baseSettings
            rootProject.name = "peer-overrides-root"
            include("peer-lib")
            """.trimIndent(),
          )
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
          file("peer-lib/build.gradle.kts").writeText(
            """
            plugins { id("com.mkobit.jenkins.pipelines.shared-library") }
            """.trimIndent(),
          )
          file("peer-lib/vars/peerStep.groovy").writeText("def call() {}")

          val result = runner(gradleVersion).withArguments("printSpecs").build()

          val specLines = result.output.lines().filterMatching { it.shouldStartWith("spec:") }
          specLines shouldHaveSize 1
          specLines.single() shouldContain ":peer-lib|renamed-in-tests|false"
        }
      }
    }

    describe("consumer src compiles against peer src symbols on compileOnly") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withTestProject {
          settingsFile.writeText(
            """
            $baseSettings
            rootProject.name = "peer-compile-root"
            include("peer-lib")
            """.trimIndent(),
          )
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
          file("peer-lib/build.gradle.kts").writeText(
            """
            plugins { id("com.mkobit.jenkins.pipelines.shared-library") }
            """.trimIndent(),
          )
          file("peer-lib/src/com/example/peer/PeerType.groovy").writeText(
            """
            package com.example.peer
            class PeerType {
                String value() { "peer-value" }
            }
            """.trimIndent(),
          )

          val result = runner(gradleVersion).withArguments("compileGroovy").build()

          result.task(":compileGroovy") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }
        }
      }
    }

    describe("missing peer plugin: clear error when project(\":lib\") does not apply the shared-library plugin") {
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withTestProject {
          settingsFile.writeText(
            """
            $baseSettings
            rootProject.name = "peer-missing-plugin-root"
            include("peer-lib")
            """.trimIndent(),
          )
          buildFile.writeText(
            """
            plugins { id("com.mkobit.jenkins.pipelines.shared-library") }
            sharedLibrary {
                dependencies {
                    sharedLibrary(project(":peer-lib"))
                }
            }
            $printResolvedTask
            """.trimIndent(),
          )
          // peer-lib applies the bare java-library plugin instead of shared-library — it has no
          // sharedLibrarySourceElements variant. Consumer resolution must fail loudly, not
          // silently produce zero peer sources.
          file("peer-lib/build.gradle.kts").writeText(
            """
            plugins { `java-library` }
            """.trimIndent(),
          )
          file("peer-lib/src/main/java/com/example/Placeholder.java").writeText(
            "package com.example; public class Placeholder {}",
          )

          val result = runner(gradleVersion).withArguments("printResolved").buildAndFail()

          // Gradle's variant-selection failure message names both the missing attribute and the
          // configurations that were considered — that's the surface we care about for diagnosis.
          result.output shouldContain "jenkins-shared-library"
        }
      }
    }

    xdescribe("binary GAV: sharedLibrary(\"group:artifact:version\") from a local Maven repo (deferred)") {
      // Blocked: the sharedLibrarySourceElements variant carries a directory artefact, which
      // Gradle's maven-publish pipeline cannot checksum / upload. Two paths forward, neither
      // implemented yet:
      //  1. Add a sibling sources-JAR variant alongside sharedLibrarySourceElements (zips the
      //     directory at publish time) plus a consumer-side ArtifactTransform that unzips back
      //     to a directory.
      //  2. Detect the presence of the directory variant at consumer resolution time and only
      //     allow project / composite paths for it, while routing GAV consumers to the sources
      //     classifier — same ArtifactTransform fallback path described in
      //     docs/06-backlog.md "External library resolution and variant selection".
      // Until either lands, attaching the variant with skip() keeps the cross-project paths
      // working; this test stays xdescribed as the unblock signal.
      withData(TestedGradleVersion.filtered) { gradleVersion ->
        withTestProject {
          val mavenRepoPath = dir.resolve("local-maven-repo").toUri().toString()

          file("producer/settings.gradle.kts").writeText(
            """
            $baseSettings
            rootProject.name = "peer-lib"
            """.trimIndent(),
          )
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
            .withArguments("publish")
            .build()

          settingsFile.writeText(
            """
            plugins {
                id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
            }
            dependencyResolutionManagement {
                repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                repositories {
                    mavenCentral()
                    maven("https://repo.jenkins-ci.org/public/")
                    maven("$mavenRepoPath")
                }
            }
            rootProject.name = "peer-binary-consumer"
            """.trimIndent(),
          )
          buildFile.writeText(
            """
            plugins { id("com.mkobit.jenkins.pipelines.shared-library") }
            sharedLibrary {
                dependencies {
                    sharedLibrary("com.example.shared:peer-lib:1.2.3")
                }
            }
            $printResolvedTask
            """.trimIndent(),
          )

          val result = runner(gradleVersion).withArguments("printResolved").build()

          val sourceLines = result.output.lines().filterMatching { it.shouldStartWith("peer-source:") }
          sourceLines shouldHaveSize 1
          sourceLines.single() shouldContain "com.example.shared:peer-lib"

          val compileLines = result.output.lines().filterMatching { it.shouldStartWith("compile:") }
          compileLines.forAtLeastOnePeer()
        }
      }
    }
  })

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
