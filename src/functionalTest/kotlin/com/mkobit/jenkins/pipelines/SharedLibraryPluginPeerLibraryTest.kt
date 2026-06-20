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
                          libraryName = "renamed-in-tests"
                          implicit = false
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

      describe("integrationTest suite: peer JAR is on compile classpath but NOT on runtime classpath") {
        forGradleVersions { gradleVersion ->
          withTestProject {
            // If peer JARs land on the runtime classpath, direct peers load via AppClassLoader
            // and stop sharing a classloader with transitive peers — breaks cross-library imports.
            settingsFile.writeText(jenkinsSettings("peer-classpath-isolation", includes = listOf("peer-lib")))
            buildFile.writeText(
              """
              plugins { id("com.mkobit.jenkins.pipelines.shared-library") }
              sharedLibrary {
                  dependencies {
                      sharedLibrary(project(":peer-lib"))
                  }
              }
              tasks.register("printIntegrationTestClasspaths") {
                  val compile = configurations.named("integrationTestCompileClasspath")
                  val runtime = configurations.named("integrationTestRuntimeClasspath")
                  doLast {
                      compile.get().files.forEach { println("itc:" + it.name) }
                      runtime.get().files.forEach { println("itr:" + it.name) }
                  }
              }
              """.trimIndent(),
            )
            file("peer-lib/build.gradle.kts").writeText(barePeerSubproject())
            file("peer-lib/vars/peerStep.groovy").writeText("def call() {}")

            val output = runner(gradleVersion).build("printIntegrationTestClasspaths").output
            val compileLines = output.lines().filter { it.startsWith("itc:") }
            val runtimeLines = output.lines().filter { it.startsWith("itr:") }

            compileLines.forOne { it shouldContain "peer-lib" }
            if (runtimeLines.any { it.contains("peer-lib") }) {
              throw AssertionError(
                "peer JAR leaked back onto integration test runtime classpath:\n${runtimeLines.joinToString("\n")}",
              )
            }
          }
        }
      }

      describe("peer library JVM args injected into the default `test` suite too (for JPU)") {
        forGradleVersions { gradleVersion ->
          withTestProject {
            // JPU lives in the default `test` suite. test.library.N.{name,location,implicit} are
            // exposed there too so JPU tests can iterate peers without hard-coding paths.
            settingsFile.writeText(jenkinsSettings("peer-jpu-args", includes = listOf("peer-lib")))
            buildFile.writeText(
              """
              plugins { id("com.mkobit.jenkins.pipelines.shared-library") }
              sharedLibrary {
                  dependencies {
                      sharedLibrary(project(":peer-lib"))
                  }
              }
              tasks.register("printTestJvmArgs") {
                  dependsOn(":peer-lib:syncSharedLibrarySource")
                  doLast {
                      tasks.named<Test>("test").get()
                          .jvmArgumentProviders
                          .flatMap { it.asArguments() }
                          .filter { it.startsWith("-Dtest.library.") }
                          .forEach { println("test-arg:" + it) }
                  }
              }
              """.trimIndent(),
            )
            file("peer-lib/build.gradle.kts").writeText(barePeerSubproject())
            file("peer-lib/vars/peerStep.groovy").writeText("def call() {}")

            val output = runner(gradleVersion).build("printTestJvmArgs").output
            val argLines = output.lines().filter { it.startsWith("test-arg:") }
            // self-library (index 0, 3 props) + one peer (index 1, 3 props)
            argLines shouldHaveSize 6
            argLines.forOne { it shouldContain "-Dtest.library.0.name=peer-jpu-args" }
            argLines.forOne { it shouldContain "-Dtest.library.1.name=peer-lib" }
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

      describe("duplicate libraryName across peers fails at task validation with a clear message") {
        forGradleVersions { gradleVersion ->
          withTestProject {
            // Without the check, duplicates silently shadow each other in Jenkins
            // (last-registration-wins) and surface later as confusing "wrong step" failures.
            settingsFile.writeText(jenkinsSettings("peer-name-collision", includes = listOf("lib-a", "lib-b")))
            buildFile.writeText(
              """
              plugins { id("com.mkobit.jenkins.pipelines.shared-library") }
              sharedLibrary {
                  dependencies {
                      sharedLibrary(project(":lib-a")) { libraryName = "shared" }
                      sharedLibrary(project(":lib-b")) { libraryName = "shared" }
                  }
              }
              """.trimIndent(),
            )
            file("vars/consumerStep.groovy").writeText("def call() {}")
            file("test/integration/java/StubIT.java").writeText(STUB_INTEGRATION_TEST)
            file("lib-a/build.gradle.kts").writeText(barePeerSubproject())
            file("lib-a/vars/aStep.groovy").writeText("def call() {}")
            file("lib-b/build.gradle.kts").writeText(barePeerSubproject())
            file("lib-b/vars/bStep.groovy").writeText("def call() {}")

            val result = runner(gradleVersion).buildAndFail("integrationTest")
            result.output shouldContain "duplicate Jenkins shared library name"
            result.output shouldContain "shared"
          }
        }
      }

      describe("peer libraryName colliding with consumer libraryName fails at task validation") {
        forGradleVersions { gradleVersion ->
          withTestProject {
            // Consumer registers at index 0 with project.name; a peer choosing the same name
            // would silently shadow it in Jenkins.
            settingsFile.writeText(jenkinsSettings("self-collision", includes = listOf("peer-lib")))
            buildFile.writeText(
              """
              plugins { id("com.mkobit.jenkins.pipelines.shared-library") }
              sharedLibrary {
                  dependencies {
                      sharedLibrary(project(":peer-lib")) { libraryName = "self-collision" }
                  }
              }
              """.trimIndent(),
            )
            file("vars/consumerStep.groovy").writeText("def call() {}")
            file("test/integration/java/StubIT.java").writeText(STUB_INTEGRATION_TEST)
            file("peer-lib/build.gradle.kts").writeText(barePeerSubproject())
            file("peer-lib/vars/peerStep.groovy").writeText("def call() {}")

            val result = runner(gradleVersion).buildAndFail("integrationTest")
            result.output shouldContain "duplicate Jenkins shared library name"
            result.output shouldContain "self-collision"
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

    describe("custom test suite with useTestHarness=true gets the same peer wiring as integrationTest") {
      forGradleVersions { gradleVersion ->
        withTestProject {
          // Guards that user-registered JvmTestSuites (Spock, Kotest, etc.) flipping
          // useTestHarness=true receive the same peer library wiring as the built-in integrationTest.
          settingsFile.writeText(jenkinsSettings("peer-custom-suite-consumer", includes = listOf("peer-lib")))
          buildFile.writeText(
            """
            import com.mkobit.jenkins.pipelines.jenkins
            plugins { id("com.mkobit.jenkins.pipelines.shared-library") }
            sharedLibrary {
                dependencies {
                    sharedLibrary(project(":peer-lib"))
                }
            }
            testing {
                suites {
                    register<JvmTestSuite>("integrationTestAlt") {
                        useJUnitJupiter()
                        sources {
                            java.setSrcDirs(listOf("test/integration-alt/java"))
                        }
                        jenkins.useTestHarness.set(true)
                    }
                }
            }
            """.trimIndent(),
          )
          file("vars/consumerStep.groovy").writeText("def call() {}")
          file("peer-lib/build.gradle.kts").writeText(barePeerSubproject())
          file("peer-lib/vars/peerGreet.groovy").writeText(
            "def call(String who) { return \"hello, \${who}\" }",
          )
          file("test/integration-alt/java/PeerAltIT.java").writeText(
            """
            import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
            import org.jenkinsci.plugins.workflow.job.WorkflowJob;
            import org.jenkinsci.plugins.workflow.job.WorkflowRun;
            import org.junit.jupiter.api.Test;
            import org.jvnet.hudson.test.JenkinsRule;
            import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

            @WithJenkins
            class PeerAltIT {
                @Test
                void customSuiteResolvesPeerVarsStep(JenkinsRule jenkins) throws Exception {
                    var job = jenkins.createProject(WorkflowJob.class);
                    job.setDefinition(new CpsFlowDefinition("echo peerGreet('alt-suite')", true));
                    var run = jenkins.buildAndAssertSuccess(job);
                    jenkins.assertLogContains("hello, alt-suite", run);
                }
            }
            """.trimIndent(),
          )

          val result = runner(gradleVersion).build("integrationTestAlt")
          result.task(":integrationTestAlt") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }
        }
      }
    }

    describe("implicit=false: pipeline must explicitly @Library the peer to call its steps") {
      forGradleVersions { gradleVersion ->
        withTestProject {
          // Two pipelines run back-to-back: one without @Library (must fail), one with (must succeed).
          settingsFile.writeText(jenkinsSettings("peer-implicit-consumer", includes = listOf("peer-lib")))
          buildFile.writeText(
            """
            plugins { id("com.mkobit.jenkins.pipelines.shared-library") }
            sharedLibrary {
                dependencies {
                    sharedLibrary(project(":peer-lib")) {
                        implicit = false
                    }
                }
            }
            """.trimIndent(),
          )
          file("vars/consumerStep.groovy").writeText("def call() {}")
          file("peer-lib/build.gradle.kts").writeText(barePeerSubproject())
          file("peer-lib/vars/explicitOnlyStep.groovy").writeText(
            "def call() { return 'explicit-loaded' }",
          )
          file("test/integration/java/ImplicitFalseIT.java").writeText(
            """
            import hudson.model.Result;
            import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
            import org.jenkinsci.plugins.workflow.job.WorkflowJob;
            import org.jenkinsci.plugins.workflow.job.WorkflowRun;
            import org.junit.jupiter.api.Test;
            import org.jvnet.hudson.test.JenkinsRule;
            import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

            @WithJenkins
            class ImplicitFalseIT {
                @Test
                void pipelineWithoutLibraryAnnotationCannotResolvePeerStep(JenkinsRule jenkins) throws Exception {
                    var job = jenkins.createProject(WorkflowJob.class);
                    job.setDefinition(new CpsFlowDefinition("echo explicitOnlyStep()", true));
                    jenkins.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0));
                }

                @Test
                void pipelineWithLibraryAnnotationLoadsPeerStep(JenkinsRule jenkins) throws Exception {
                    var job = jenkins.createProject(WorkflowJob.class);
                    job.setDefinition(new CpsFlowDefinition(
                        "@Library('peer-lib') _\necho explicitOnlyStep()", true));
                    var run = jenkins.buildAndAssertSuccess(job);
                    jenkins.assertLogContains("explicit-loaded", run);
                }
            }
            """.trimIndent(),
          )

          val result = runner(gradleVersion).build("integrationTest")
          result.task(":integrationTest") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }
        }
      }
    }

    describe("peer resources/: pipeline can read a resource file from a peer via libraryResource") {
      forGradleVersions { gradleVersion ->
        withTestProject {
          // resources/ in a peer library are loaded by Jenkins via the libraryResource step.
          // Verifies the sync wiring covers resources/ in addition to src/ and vars/.
          settingsFile.writeText(jenkinsSettings("peer-resources-consumer", includes = listOf("peer-lib")))
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
          file("vars/consumerStep.groovy").writeText("def call() {}")
          file("peer-lib/build.gradle.kts").writeText(barePeerSubproject())
          file("peer-lib/vars/peerHello.groovy").writeText("def call() {}")
          file("peer-lib/resources/com/example/greeting.txt").writeText("hello from peer resource")
          file("test/integration/java/PeerResourceIT.java").writeText(
            """
            import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
            import org.jenkinsci.plugins.workflow.job.WorkflowJob;
            import org.jenkinsci.plugins.workflow.job.WorkflowRun;
            import org.junit.jupiter.api.Test;
            import org.jvnet.hudson.test.JenkinsRule;
            import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

            @WithJenkins
            class PeerResourceIT {
                @Test
                void pipelineReadsResourceFromPeer(JenkinsRule jenkins) throws Exception {
                    var job = jenkins.createProject(WorkflowJob.class);
                    job.setDefinition(new CpsFlowDefinition(
                        "echo libraryResource('com/example/greeting.txt')", true));
                    var run = jenkins.buildAndAssertSuccess(job);
                    jenkins.assertLogContains("hello from peer resource", run);
                }
            }
            """.trimIndent(),
          )

          val result = runner(gradleVersion).build("integrationTest")
          result.task(":integrationTest") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }
        }
      }
    }

    describe("integrationTest end-to-end: pipeline calls a peer's vars step") {
      forGradleVersions { gradleVersion ->
        withTestProject {
          settingsFile.writeText(jenkinsSettings("peer-e2e-consumer", includes = listOf("peer-lib")))
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
          // Consumer must have at least one src/ or vars/ file because the autoregistrar
          // registers the consumer itself as a library, and Jenkins rejects empty libraries.
          file("vars/consumerStep.groovy").writeText("def call() {}")
          file("peer-lib/build.gradle.kts").writeText(barePeerSubproject())
          file("peer-lib/vars/peerGreet.groovy").writeText(
            "def call(String who) { return \"hello, \${who}\" }",
          )
          file("test/integration/java/PeerE2EIT.java").writeText(
            """
            import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
            import org.jenkinsci.plugins.workflow.job.WorkflowJob;
            import org.jenkinsci.plugins.workflow.job.WorkflowRun;
            import org.junit.jupiter.api.Test;
            import org.jvnet.hudson.test.JenkinsRule;
            import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

            @WithJenkins
            class PeerE2EIT {
                @Test
                void pipelineCallsPeerVarsStep(JenkinsRule jenkins) throws Exception {
                    var job = jenkins.createProject(WorkflowJob.class);
                    job.setDefinition(new CpsFlowDefinition("echo peerGreet('world')", true));
                    var run = jenkins.buildAndAssertSuccess(job);
                    jenkins.assertLogContains("hello, world", run);
                }
            }
            """.trimIndent(),
          )

          val result = runner(gradleVersion).build("integrationTest")
          result.task(":integrationTest") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }
        }
      }
    }

    describe("bidirectional cross-library src/ refs fail with a Gradle circular dependency") {
      forGradleVersions { gradleVersion ->
        withTestProject {
          // Two peers each importing the other's src/ class. Jenkins would resolve at runtime
          // (one shared CleanGroovyClassLoader) but Gradle can't schedule the compile graph.
          settingsFile.writeText(jenkinsSettings("peer-bidirectional", includes = listOf("lib-a", "lib-b")))
          buildFile.writeText(AGGREGATOR_BUILD_FILE)

          file("lib-a/build.gradle.kts").writeText(barePeerSubproject(declaresPeerProjects = listOf(":lib-b")))
          file("lib-a/src/com/example/AClass.groovy").writeText(
            """
            package com.example
            class AClass implements Serializable { String greet() { new BClass().reply() } }
            """.trimIndent(),
          )
          file("lib-b/build.gradle.kts").writeText(barePeerSubproject(declaresPeerProjects = listOf(":lib-a")))
          file("lib-b/src/com/example/BClass.groovy").writeText(
            """
            package com.example
            class BClass implements Serializable { String reply() { new AClass().toString() } }
            """.trimIndent(),
          )

          val result = runner(gradleVersion).buildAndFail(":lib-a:compileGroovy")
          result.output shouldContain "Circular dependency"
          result.output shouldContain ":lib-a:compileGroovy"
          result.output shouldContain ":lib-b:compileGroovy"
        }
      }
    }

    describe("cross-library src/ import: peer src class is visible to another peer's vars at Jenkins runtime") {
      forGradleVersions { gradleVersion ->
        withTestProject {
          // Real Jenkins gives every library in a pipeline run the same CpsGroovyShell
          // CleanGroovyClassLoader, so a class compiled from one peer's src/ is visible to
          // another peer's vars script via a plain import — no @Library, no merged sources.
          // The leak fix is what makes this provable in our integration tests.
          settingsFile.writeText(jenkinsSettings("cross-src-root", includes = listOf("src-lib", "step-lib")))
          buildFile.writeText(AGGREGATOR_BUILD_FILE)

          file("src-lib/build.gradle.kts").writeText(barePeerSubproject())
          file("src-lib/src/com/example/CrossClass.groovy").writeText(
            $$"""
            package com.example
            class CrossClass implements Serializable {
                String value(String input) { "cross: $input" }
            }
            """.trimIndent(),
          )

          file("step-lib/build.gradle.kts").writeText(barePeerSubproject(declaresPeerProjects = listOf(":src-lib")))
          file("step-lib/vars/crossStep.groovy").writeText(
            """
            import com.example.CrossClass
            def call(String input) { return new CrossClass().value(input) }
            """.trimIndent(),
          )

          file("step-lib/test/integration/java/CrossSrcIT.java").writeText(
            """
            import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
            import org.jenkinsci.plugins.workflow.job.WorkflowJob;
            import org.jenkinsci.plugins.workflow.job.WorkflowRun;
            import org.junit.jupiter.api.Test;
            import org.jvnet.hudson.test.JenkinsRule;
            import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

            @WithJenkins
            class CrossSrcIT {
                @Test
                void varsCanImportPeerSrcClassNatively(JenkinsRule jenkins) throws Exception {
                    var job = jenkins.createProject(WorkflowJob.class);
                    job.setDefinition(new CpsFlowDefinition("echo crossStep('hello')", true));
                    var run = jenkins.buildAndAssertSuccess(job);
                    jenkins.assertLogContains("cross: hello", run);
                }
            }
            """.trimIndent(),
          )

          val result = runner(gradleVersion).build(":step-lib:integrationTest")
          result.task(":step-lib:integrationTest") shouldNotBeNull { outcome shouldBe TaskOutcome.SUCCESS }
        }
      }
    }

    // Tracked in https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/165
    // sharedLibrarySourceElements ships a directory artifact that Maven's publishing pipeline cannot
    // checksum or upload, so the variant metadata never lands in a repo. Unblocking this requires a
    // publish-side sources-JAR variant and a consumer-side ArtifactTransform to unzip it back to a
    // directory — a non-trivial design given that JSL source is not compiled before publishing.
    xdescribe("binary GAV (blocked on sources-JAR + ArtifactTransform — see issue #165)") {
      xdescribe("""sharedLibrary("group:artifact:version") from a local Maven repo""") {
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

private val AGGREGATOR_BUILD_FILE = """plugins { id("com.mkobit.jenkins.pipelines.shared-library") apply false }"""

// Minimal integration test that forces integrationTest to have at least one test case, so the
// task's jvmArgumentProviders actually fire (NO-SOURCE skips them entirely).
private val STUB_INTEGRATION_TEST =
  """
  import org.junit.jupiter.api.Test;
  class StubIT { @Test void noop() {} }
  """.trimIndent()

private fun rootBuildFile(sharedLibraryBody: String): String =
  """
  plugins { id("com.mkobit.jenkins.pipelines.shared-library") }
  ${sharedLibraryBody.trimIndent()}
  $PRINT_RESOLVED_TASK
  """.trimIndent()

private fun barePeerSubproject(declaresPeerProjects: List<String> = emptyList()): String {
  val deps = declaresPeerProjects.joinToString("\n") { """        sharedLibrary(project("$it"))""" }
  return if (deps.isEmpty()) {
    """plugins { id("com.mkobit.jenkins.pipelines.shared-library") }"""
  } else {
    """
    plugins { id("com.mkobit.jenkins.pipelines.shared-library") }
    sharedLibrary {
        dependencies {
    $deps
        }
    }
    """.trimIndent()
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
