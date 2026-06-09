# Jenkins pipeline shared library Gradle plugin

[![Plugin Version](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com/mkobit/jenkins/pipelines/jenkins-pipeline-shared-libraries-gradle-plugin/maven-metadata.xml.svg?label=Gradle%20Plugin%20Portal)](https://plugins.gradle.org/plugin/com.mkobit.jenkins.pipelines.shared-library)
[![Build](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/actions/workflows/build.yml)
[![License](https://img.shields.io/github/license/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin)](LICENSE.txt)

| | Tested versions |
|---|---|
| ![Gradle](https://img.shields.io/badge/Gradle--green?logo=gradle) | ![9.4.0](https://img.shields.io/badge/9.4.0--green) ![9.4.1](https://img.shields.io/badge/9.4.1--green) ![9.5.0](https://img.shields.io/badge/9.5.0--green) ![9.5.1](https://img.shields.io/badge/9.5.1--green) |
| ![Java](https://img.shields.io/badge/Java--orange?logo=openjdk) | ![17](https://img.shields.io/badge/17--orange) ![21](https://img.shields.io/badge/21--orange) ![25](https://img.shields.io/badge/25--orange) |
| ![Jenkins LTS](https://img.shields.io/badge/Jenkins_LTS--blue?logo=jenkins) | ![2.479.x](https://img.shields.io/badge/2.479.x--blue) ![2.528.x](https://img.shields.io/badge/2.528.x--blue) ![2.541.x](https://img.shields.io/badge/2.541.x--blue) |

A Gradle plugin for developing and testing [Jenkins Pipeline Shared Libraries](https://jenkins.io/doc/book/pipeline/shared-libraries/).

## Features

- Groovy compilation of `src/` and `vars/` against Jenkins APIs.
- Unit testing via [Jenkins Pipeline Unit](https://github.com/lesfurets/JenkinsPipelineUnit).
- Integration testing via [Jenkins Test Harness](https://github.com/jenkinsci/jenkins-test-harness) (`JenkinsRule`).
- Automatic Jenkins BOM injection and dependency alignment.
- Local library auto-registration for integration tests (no network needed).
- Configuration cache compliant.

## Quick start

`gradle/libs.versions.toml`

```toml
[plugins]
jenkins-shared-library = { id = "com.mkobit.jenkins.pipelines.shared-library", version = "VERSION" }
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
    dependencies {
        sharedLibrary(project(":peer-lib"))          // peer shared library (multi-project)
        sharedLibrary("com.example:config-lib:1.0") // peer via composite build (includeBuild)
    }
    pipelineUnitVersion = "1.29"                    // JenkinsPipelineUnit version (test suite)
    libraryName = "my-shared-lib"                   // Jenkins library name (default: project.name)
    autoRegisterLibrary = true                      // generate SharedLibraryAutoRegistrar (default: true)
    implicit = true                                 // register library as implicit (default: true)
}
```

The Jenkins BOM for the configured LTS line is injected automatically into `jenkinsPlugin` — no explicit `jenkinsPlugin(platform(...))` call is needed.
The BOM module coordinate is derived from `jenkins.version` (e.g., `2.479.1` → `bom-2.479.x`).
The `plugins {}` block is equivalent to `dependencies { jenkinsPlugin("...") }` — use whichever reads more naturally in your build.

## Examples

The [`examples/`](examples/) directory contains standalone Gradle composite builds demonstrating common usage patterns.

| Example | Demonstrates |
|---|---|
| [`basic`](examples/basic) | Minimal plugin apply; JenkinsPipelineUnit unit tests and `JenkinsRule` integration tests |
| [`additional-test-suites`](examples/additional-test-suites) | Custom third test suite wired via `sharedLibrary.withJenkins()` |
| [`explicit-library-name`](examples/explicit-library-name) | `libraryName` override and `implicit = false` |
| [`junit-groovy`](examples/junit-groovy) | Unit tests written in Groovy using JenkinsPipelineUnit |
| [`kotest`](examples/kotest) | Kotlin source with Kotest unit and integration test suites |
| [`library-resource`](examples/library-resource) | Steps that read files via `libraryResource()` |
| [`peer-libraries`](examples/peer-libraries) | Declaring another shared library as a peer dependency for cross-library step access |
| [`peer-libraries-composite`](examples/peer-libraries-composite) | Peer libraries across separate Gradle builds via `includeBuild` and GAV notation; transitive nested composite |
| [`version-catalog`](examples/version-catalog) | Version catalog wiring for plugin versions and Jenkins plugin coordinates |

Run all examples from the repo root:

```shell
./gradlew :examples:check
```

For a complete standalone example see the [example repository](https://github.com/mkobit/jenkins-pipeline-shared-library-example).

## Running tests

```shell
./gradlew test             # JenkinsPipelineUnit unit tests
./gradlew integrationTest  # JenkinsRule integration tests (downloads Jenkins WAR on first run)
./gradlew check            # all suites
```

Jenkins downloads the WAR and plugins on first run; subsequent runs use the Gradle module cache.

## Changing the Jenkins LTS line

Set `version` in `sharedLibrary {}` to target a different LTS line:

```kotlin
sharedLibrary {
    jenkins {
        version = "2.528.3"
    }
}
```

The plugin derives the BOM module coordinate automatically from `version` (e.g., `2.528.3` → `bom-2.528.x`).
Renovate keeps the BOM version up to date within the configured LTS line.
To override the BOM version explicitly, set `bomVersion` as well.

## Additional test suites

Register extra suites and opt them into full Jenkins wiring with `withJenkins()`.
This applies the same wiring as the built-in `integrationTest` suite: `jenkins-test-harness`, HPI classpath, WAR path, system properties, JVM `--add-opens` flags, `maxParallelForks = 1`, and heap defaults.

See the [`additional-test-suites`](examples/additional-test-suites) example.

Wire additional suites into `check` if they should run in CI:

```kotlin
tasks.check {
    dependsOn(
        tasks.named("smokeTest"),
    )
}
```

> [!NOTE]
> Spock 2.x brings Groovy 3.x onto the runtime classpath.
> On Jenkins 2.479.x LTS this conflicts with the bundled `groovy-all:2.4.21` when `sandbox=true`.
> Use `sandbox=false` in `CpsFlowDefinition` for test suites on 2.479.x.
> This restriction is expected to lift on Jenkins 2.492.x+ once its internal Groovy 3 migration completes.

## Peer libraries

A shared library can declare other shared libraries as peer dependencies so their steps and classes are available during integration tests.
Peer libraries are registered in the embedded Jenkins runtime alongside the project's own library — no manual `GlobalLibraries` wiring is needed.

```kotlin
sharedLibrary {
    dependencies {
        sharedLibrary(project(":peer-lib"))                    // subproject in the same build
        sharedLibrary("com.example:config-lib:1.0.0")         // composite build (includeBuild)
        sharedLibrary(project(":config-lib")) {
            libraryName.set("config")   // override the Jenkins library name
            implicit.set(false)         // require @Library('config') _ in pipelines
        }
    }
}
```

Peer library classes are available on `compileOnly` for symbol resolution in the consuming library's Groovy source.
Peer library source directories (`src/`, `vars/`, `resources/`) are injected into the embedded Jenkins at integration-test time via `test.library.N.*` system properties.

> [!NOTE]
> Binary GAV coordinates (`"group:artifact:version"`) work when the peer is declared via `includeBuild(...)` in `settings.gradle.kts` and Gradle substitutes the coordinate with the local project.
> Resolution from a remote Maven repository is not supported: the `sharedLibrarySourceElements` variant ships a directory artifact that Maven's publishing pipeline cannot upload.
> See [issue #165](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/165).

See the [`peer-libraries`](examples/peer-libraries) example.

## JUnit 4

The built-in `integrationTest` suite defaults to JUnit Jupiter.
If you have an existing JUnit 4 test suite, configure it explicitly:

```kotlin
testing {
    suites {
        named<JvmTestSuite>("integrationTest") {
            useJUnit()
            dependencies {
                implementation(libs.junit)
            }
        }
    }
}
```

## Migration from 0.10.x

See the [0.11.0 entry in CHANGELOG.md](CHANGELOG.md) for the full API diff and the bundled OpenRewrite migration recipe.

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
