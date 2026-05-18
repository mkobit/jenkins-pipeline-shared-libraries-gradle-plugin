# Jenkins pipeline shared library Gradle plugin

[![Plugin Version](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com/mkobit/jenkins/pipelines/jenkins-pipeline-shared-libraries-gradle-plugin/maven-metadata.xml.svg?label=Gradle%20Plugin%20Portal)](https://plugins.gradle.org/plugin/com.mkobit.jenkins.pipelines.shared-library)
[![Build](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/actions/workflows/build.yml)

> [!NOTE]
> This documentation tracks the `HEAD` of the repository.
> For a specific released version see the [GitHub Releases page](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/releases).

A Gradle plugin for developing and testing [Jenkins Pipeline Shared Libraries](https://jenkins.io/doc/book/pipeline/shared-libraries/).

## Features

- Groovy compilation of `src/` and `vars/` against Jenkins APIs.
- Unit testing via [Jenkins Pipeline Unit](https://github.com/lesfurets/JenkinsPipelineUnit).
- Integration testing via [Jenkins Test Harness](https://github.com/jenkinsci/jenkins-test-harness) (`JenkinsRule`).
- Automatic Jenkins BOM injection and dependency alignment.
- Local library auto-registration for integration tests (no network needed).
- Configuration cache compliant.

## Compatibility

![Gradle](https://img.shields.io/badge/Gradle-9.4.x_%7C_9.5.x-02303A?logo=gradle&logoColor=white)
![Java](https://img.shields.io/badge/Java-17_%7C_21_%7C_25-ED8B00?logo=openjdk&logoColor=white)
![Jenkins LTS](https://img.shields.io/badge/Jenkins_LTS-2.479.x_%7C_2.528.x_%7C_2.541.x-D24939?logo=jenkins&logoColor=white)

| Dimension | Tested versions |
|---|---|
| Gradle | 9.4.0, 9.4.1, 9.5.0, 9.5.1 |
| Java | 17, 21, 25 |
| Jenkins LTS | 2.479.x, 2.528.x, 2.541.x |

## Quick start

`gradle/libs.versions.toml`

```toml
[plugins]
jenkins-shared-library = { id = "com.mkobit.jenkins.pipelines.shared-library", version = "0.11.0" }
```

`build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.jenkins.shared.library)
}

dependencies {
    jenkinsPlugin("org.jenkinsci.plugins:pipeline-model-definition")
}
```

The plugin configures by convention:

- `src/` and `vars/` compile as Groovy against Jenkins core and the default workflow plugins
- `test/unit/` → `test` suite with JenkinsPipelineUnit on the classpath
- `test/integration/` → `integrationTest` suite with `jenkins-test-harness`

No `sharedLibrary {}` block is required for the default configuration.
The default Jenkins line is 2.479.x LTS — see [Changing the Jenkins LTS line](#changing-the-jenkins-lts-line) to target a different version.

## Source layout

Jenkins SCM loading imposes hard constraints on the main source directories.
When Jenkins loads a shared library, it places `src/` directly on the Groovy classpath, so classes must be rooted there (`src/com/example/Util.groovy` → `com.example.Util`).
`vars/` and `resources/` must also sit at the repository root.

```
src/                              ← Groovy shared library classes
vars/                             ← pipeline step scripts (filename = step name)
resources/                        ← files accessible via libraryResource()
test/
  unit/groovy/                    ← JenkinsPipelineUnit (fast, no Jenkins runtime)
  unit/java/                      ← Java unit tests (optional)
  integration/groovy/             ← JenkinsRule integration tests (embedded Jenkins)
  integration/java/               ← Java integration tests (optional)
```

Test sources may also be in `test/unit/kotlin/` or `test/integration/kotlin/` for Kotlin consumers.

## `sharedLibrary {}` extension

All properties have sensible defaults and are optional.

```kotlin
sharedLibrary {
    jenkins {
        version = "2.528.3"                         // Jenkins core version (default: 2.479.1)
        bomVersion = "6398.v1d26a_dd495e2"          // BOM auto-injected into jenkinsPlugin
    }
    plugins {
        plugin("org.jenkins-ci.plugins:git")        // additional Jenkins plugins
    }
    pipelineUnitVersion = "1.29"                    // JenkinsPipelineUnit version (test suite)
    libraryName = "my-shared-lib"                   // Jenkins library name (default: project.name)
    autoRegisterLibrary = true                      // generate SharedLibraryAutoRegistrar (default: true)
}
```

The Jenkins BOM for the configured LTS line is injected automatically into `jenkinsPlugin` — no explicit `jenkinsPlugin(platform(...))` call is needed.
The BOM module coordinate is derived from `jenkins.version` (e.g., `2.479.1` → `bom-2.479.x`).
The `plugins {}` block is equivalent to `dependencies { jenkinsPlugin("...") }` — use whichever reads more naturally in your build.

## Writing unit tests

Unit tests live in `test/unit/` and run with [Jenkins Pipeline Unit](https://github.com/lesfurets/JenkinsPipelineUnit).
They are fast, classpath-only tests with no embedded Jenkins runtime.

```groovy
// test/unit/groovy/com/example/DoStuffSpec.groovy
import com.lesfurets.jenkins.unit.BasePipelineTest

class DoStuffSpec extends BasePipelineTest {
    def 'doStuff calls echo'() {
        given:
        helper.registerAllowedMethod('echo', [String]) {}
        def script = loadScript('vars/doStuff.groovy')

        when:
        script.call('hello')

        then:
        helper.callStack.any { it.methodName == 'echo' }
    }
}
```

## Writing integration tests

Integration tests live in `test/integration/` and run against an embedded Jenkins instance.
The plugin generates `LocalLibraryRetriever.java` into the `integrationTest` source set and auto-registers the local library via `SharedLibraryAutoRegistrar` — no manual `GlobalLibraries` setup is required.

### JUnit 4 (Java)

```java
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class MyStepTest {
    @Rule public JenkinsRule rule = new JenkinsRule();

    @Test
    public void myStepRuns() throws Exception {
        WorkflowJob job = rule.createProject(WorkflowJob.class, "test");
        job.setDefinition(new CpsFlowDefinition("myStep()", true));
        rule.buildAndAssertSuccess(job);
    }
}
```

### JUnit 4 (Groovy / `GroovyJenkinsRule`)

```groovy
import org.junit.Rule
import org.junit.Test
import org.jvnet.hudson.test.GroovyJenkinsRule

class MyStepTest {
    @Rule public GroovyJenkinsRule rule = new GroovyJenkinsRule()

    @Test
    void myStepRuns() {
        def job = rule.createProject(WorkflowJob, "test")
        job.setDefinition(new CpsFlowDefinition("myStep()", true))
        rule.buildAndAssertSuccess(job)
    }
}
```

> [!NOTE]
> The `@Library` annotation in the pipeline script is not required when `autoRegisterLibrary = true` (the default) and you use the library name returned by `LocalLibraryRetriever.implicitLibrary()`.
> The library is registered at embedded Jenkins startup under the name configured in `sharedLibrary.libraryName`.

### Opting out of auto-registration

```kotlin
sharedLibrary {
    autoRegisterLibrary = false
}
```

With auto-registration disabled, register the library manually in each test:

```java
import com.mkobit.jenkins.pipelines.LocalLibraryRetriever;
import org.jenkinsci.plugins.workflow.libs.GlobalLibraries;
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration;

LibraryConfiguration lib = LocalLibraryRetriever.implicitLibrary();
GlobalLibraries.get().setLibraries(List.of(lib));
```

## Additional test suites

Register extra suites and opt them into full Jenkins wiring with `withJenkins()`.
This applies the same wiring as the built-in `integrationTest` suite: `jenkins-test-harness`, HPI classpath, WAR path, system properties, JVM `--add-opens` flags, `maxParallelForks = 1`, and heap defaults.

### JUnit Jupiter

```kotlin
testing {
    suites {
        register<JvmTestSuite>("integrationTestJunit5") {
            sharedLibrary.withJenkins(this)
            sources { java.setSrcDirs(listOf("test/integration-junit5/java")) }
            dependencies {
                implementation(libs.junit.jupiter.api)
                runtimeOnly(libs.junit.jupiter.engine)
                runtimeOnly(libs.junit.platform.launcher)
            }
            targets.all { testTask.configure { useJUnitPlatform() } }
        }
    }
}
```

### Spock 2.x

```kotlin
testing {
    suites {
        register<JvmTestSuite>("integrationTestSpock") {
            sharedLibrary.withJenkins(this)
            sources { groovy.setSrcDirs(listOf("test/integration-spock/groovy")) }
            dependencies {
                implementation(libs.spock.core)
                compileOnly(libs.groovy.core)
            }
        }
    }
}
```

> [!NOTE]
> Spock 2.x brings Groovy 3.x onto the runtime classpath.
> On Jenkins 2.479.x LTS this conflicts with the bundled `groovy-all:2.4.21` when `sandbox=true`.
> Use `sandbox=false` in `CpsFlowDefinition` for Spock suites on 2.479.x.
> This restriction is expected to lift on Jenkins 2.492.x+ once its internal Groovy 3 migration completes.

### Kotest

```kotlin
testing {
    suites {
        register<JvmTestSuite>("integrationTestKotest") {
            sharedLibrary.withJenkins(this)
            useJUnitJupiter()
            sources { extensions.configure<SourceDirectorySet>("kotlin") {
                setSrcDirs(listOf("test/integration-kotest/kotlin"))
            }}
            dependencies {
                implementation(libs.kotest.runner)
                implementation(libs.kotest.assertions)
                implementation(libs.coroutines.core)
            }
        }
    }
}
```

Wire additional suites into `check` if they should run in CI:

```kotlin
tasks.check {
    dependsOn(
        tasks.named("integrationTestJunit5"),
        tasks.named("integrationTestSpock"),
        tasks.named("integrationTestKotest"),
    )
}
```

## Running tests

```shell
./gradlew test             # JenkinsPipelineUnit unit tests
./gradlew integrationTest  # JenkinsRule integration tests (downloads Jenkins WAR on first run)
./gradlew check            # all suites
```

Jenkins downloads the WAR and plugins on first run; subsequent runs use the Gradle module cache.

## Changing the Jenkins LTS line

To upgrade the Jenkins LTS line:

1. Update `jenkins-bom` in `gradle/libs.versions.toml` to the new module and version:

   ```toml
   [libraries]
   jenkins-bom = { module = "io.jenkins.tools.bom:bom-2.528.x", version.ref = "jenkins-bom" }
   ```

2. Optionally pin the Jenkins core version in `sharedLibrary {}` to target a specific minor release:

   ```kotlin
   sharedLibrary {
       jenkins {
           version = "2.528.3"
       }
   }
   ```

Renovate keeps the BOM version up to date within the pinned LTS line.
Changing the LTS module coordinate is a manual step.

## Migration from 0.10.x

Version 0.11.0 is a clean break from the 0.10.x series.
The following table maps old API to its replacement.

| 0.10.x | 0.11.0 |
|---|---|
| `sharedLibrary { pluginDependencies { dependency("git") { ... } } }` | `dependencies { jenkinsPlugin("org.jenkins-ci.plugins:git") }` |
| `sharedLibrary { coreVersion.set("2.222.4") }` | `sharedLibrary { jenkins { version = "2.528.3" } }` or let the BOM default apply |
| `sharedLibrary { pipelineTestUnitVersion.set("...") }` | `sharedLibrary { pipelineUnitVersion = "..." }` |
| `sharedLibrary { testHarnessVersion.set("...") }` | Removed — managed by the Jenkins BOM; to override, add `implementation("org.jenkins-ci.main:jenkins-test-harness:VERSION")` in the suite's `dependencies` block |
| Named `*Version` properties on `PluginDependencySpec` | Removed — declare versions in `gradle/libs.versions.toml`; use BOM for Jenkins plugins |
| `workflowCpsPluginVersion`, `workflowJobPluginVersion`, … | Removed — these plugins are managed by the BOM |
| Custom configurations (`jenkinsPlugins`, `jenkinsPluginHpisAndJpis`, …) | `jenkinsPlugin` is the single user-facing configuration |

An OpenRewrite migration recipe is bundled in the plugin JAR:

```kotlin
// build.gradle.kts (migration only)
plugins {
    id("org.openrewrite.rewrite") version "6.x"
}
rewrite {
    activeRecipe("com.mkobit.jenkins.pipelines.MigrateSharedLibraryPlugin010To011")
}
```

The recipe automates: plugin version bump, `jenkinsPlugins` → `jenkinsPlugin` rename, and two other configuration renames.
Extension restructuring and BOM setup require manual steps documented in `MigrateSharedLibraryPlugin010To011Full`.

## Example consumer

See the [example repository](https://github.com/mkobit/jenkins-pipeline-shared-library-example) for a complete project using all supported test frameworks (JUnit 4, JUnit 5, Spock 2.x, Kotest) against a real Jenkins instance.

## Troubleshooting

### Jenkins WAR not found at runtime

Symptom: `WarExploder` or `JenkinsRule` fails with "WAR not found".
The plugin injects `jth.jenkins-war.path` automatically.
If you see this error, verify that `integrationTest` is configured by the plugin (not manually) and that `jenkins-war` is on the `jenkinsPlugin` configuration.

### `groovy-all` conflict with `sandbox=true` and Spock 2.x on Jenkins 2.479.x

The plugin injects `groovy-all:2.4.21` at integration test runtime to satisfy `SandboxInterceptor`.
Spock 2.x also brings `groovy:3.x` onto the classpath.
These conflict when `sandbox=true`.
Use `sandbox=false` on 2.479.x LTS, or move to a 2.492.x+ Jenkins line where the internal Groovy runtime is 3.x.

### `@Grab` in shared library source

`@Grab` annotations resolve at Jenkins runtime via Grape/Ivy — not at Gradle build time.
Gradle's `compileGroovy` task runs in an isolated classloader that cannot resolve `@Grab` dependencies.
For tests that exercise `@Grab`-annotated code, use `JenkinsRule` (`integrationTest`) with network access, or set up a local Ivy repository pointed at the Gradle module cache.

### `ClassFilter` errors with custom `LocalLibraryRetriever`

The generated `META-INF/hudson.remoting.ClassFilter` resource whitelists `LocalLibraryRetriever` for Jenkins remoting.
If you see `ClassFilter` rejections for the generated class, ensure `generateLocalLibraryFiles` has run (it is wired as a dependency of `compileIntegrationTestJava` automatically).
