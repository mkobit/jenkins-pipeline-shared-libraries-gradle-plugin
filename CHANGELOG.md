# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.11.0](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/compare/v0.10.1...v0.11.0) (2026-05-19)

> [!IMPORTANT]
> **This is a complete rewrite** — the first release since 0.10.1 (July 2019), nearly seven years later.
> The plugin has been rebuilt from the ground up for Gradle 9.x, Java 17/21, and modern Jenkins LTS lines.
> All 0.10.x APIs have been removed.
> See the [migration guide in the README](README.md#migration-from-010x) or run the bundled OpenRewrite recipe.

### Added

- Single `jenkinsPlugin` configuration for all plugin dependencies — replaces the old multi-configuration model.
- Jenkins BOM auto-injection — no explicit `jenkinsPlugin(platform(...))` call is needed; the BOM coordinate is derived from `jenkins.version` (e.g., `2.479.1` → `bom-2.479.x`).
- `sharedLibrary.plugins { plugin("group:artifact") }` DSL block for declaring Jenkins plugin dependencies inline inside the extension; equivalent to `dependencies { jenkinsPlugin("...") }`.
- `sharedLibrary.withJenkins(suite)` for opt-in Jenkins test-harness wiring on additional `JvmTestSuite` registrations (JUnit Jupiter, Spock 2.x, Kotest, or any framework).
- `autoRegisterLibrary` property (default: `true`) — generates `SharedLibraryAutoRegistrar` and registers the library in embedded Jenkins at startup; no `GlobalLibraries.get().setLibraries(...)` call needed in tests.
- `libraryName` property (default: `project.name`) — controls the Jenkins library identifier used in `@Library("...")` pipeline scripts and `LocalLibraryRetriever.implicitLibrary()`.
- Generated `LocalLibraryRetriever` class — loads the local shared library in integration tests without network access.
- Built-in Jenkins CodeNarc rules (`codenarcJenkinsMain` task) — validates CPS-safety and `@Serializable` annotations on shared library sources.
- OpenRewrite migration recipe `com.mkobit.jenkins.pipelines.MigrateSharedLibraryPlugin010To011` for automated 0.10.x → 0.11.x migration.
- Configuration cache support.
- Java 17, 21, and 25 toolchain support.
- Jenkins LTS 2.479.x, 2.528.x, and 2.541.x compatibility with full BOM alignment.
- `syncSharedLibrarySource` task — syncs `src/`, `vars/`, and `resources/` into `build/sharedLibrarySource/{libraryName}/` as a cacheable, incremental operation.
- `sharedLibrarySourceElements` outgoing variant — exposes the synced source directory as a Gradle variant with `Category` and `Usage` attributes for cross-project resolution.

### Changed

- Minimum Gradle version is now 9.x (previously 4.x–8.x).
- Minimum Java version is now 17 (previously 8).
- Minimum Jenkins LTS is now 2.479.x.
- `sharedLibrary { coreVersion }` replaced by `sharedLibrary { jenkins { version = "..." } }`.
- `sharedLibrary { pipelineTestUnitVersion }` renamed to `sharedLibrary { pipelineUnitVersion }`.

### Removed

- `sharedLibrary { pluginDependencies { dependency(...) } }` — use `dependencies { jenkinsPlugin("group:artifact") }` or `sharedLibrary { plugins { plugin("group:artifact") } }`.
- `sharedLibrary { testHarnessVersion }` — managed by the Jenkins BOM; to override, add `implementation("org.jenkins-ci.main:jenkins-test-harness:VERSION")` in the suite's `dependencies` block.
- Named `*Version` properties on `PluginDependencySpec` — declare versions in `gradle/libs.versions.toml` and use the BOM for Jenkins plugins.
- All individual workflow plugin version properties (`workflowCpsPluginVersion`, `workflowJobPluginVersion`, etc.) — these plugins are managed by the Jenkins BOM.
- Custom configurations (`jenkinsPlugins`, `jenkinsPluginHpisAndJpis`, etc.) — `jenkinsPlugin` is the only user-facing configuration.

### Usage

`settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}
```

`build.gradle.kts`

```kotlin
plugins {
    id("com.mkobit.jenkins.pipelines.shared-library") version "0.11.0"
}

sharedLibrary {
    jenkins {
        version = "2.479.1"
    }
    plugins {
        plugin("org.jenkins-ci.plugins:pipeline-model-definition")
    }
}
```

Source layout:

```
src/com/example/Util.groovy
vars/myStep.groovy
resources/com/example/data.json
test/unit/groovy/com/example/UtilSpec.groovy
test/integration/groovy/com/example/MyStepTest.groovy
```

For a complete working project, see the example repository:

- [at `c376906`](https://github.com/mkobit/jenkins-pipeline-shared-library-example/tree/c37690649d10aa7cabdd534062bde5a5560ce852) — pinned to the version tested against 0.11.0
- [latest `main`](https://github.com/mkobit/jenkins-pipeline-shared-library-example/tree/main)

---

> [!NOTE]
> Entries below are preserved from the pre-revamp plugin (versions 0.10.x and earlier).
> The project was substantially rewritten starting from 0.11.0 — see [the phoenix project PR](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/pull/124) for full context.

## [0.10.1] - 2019-07-30

### Changed

- Remove usage of deprecated `layout.directoryProperty` in `JenkinsIntegrationPlugin`

## [0.10.0] - 2019-07-26

- Java 11 compatibility with Java 11 compatible Jenkins versions and plugins

### Changed

- Replaced usage of `javax.annotation.Generated` with `com.mkobit.jenkins.pipelines.codegen.JenkinsGradlePluginSharedLibraryGenerated`.
  This enables Java 11 compatibility with Java 11 compatible Jenkins versions.
- Updated default versions:

  | Component | From | To |
  |---|---|---|
  | Core | 2.164.3 | 2.176.2 |
  | Test Harness | 2.49 | 2.54 |
  | Workflow API Plugin | 2.34 | 2.35 |
  | Workflow Basic Steps Plugin | 2.16 | 2.18 |
  | Workflow CPS Plugin | 2.68 | 2.72 |
  | Workflow Global CPS Library Plugin | 2.13 | 2.14 |
  | Workflow Durable Task Step Plugin | 2.30 | 2.32 |
  | Workflow Job Plugin | 2.32 | 2.33 |
  | Workflow SCM Step Plugin | 2.7 | 2.9 |
  | Workflow Step API Plugin | 2.19 | 2.20 |

### Fixed

- Deprecation warning in `GenerateJavaFile`

## [0.9.1] - 2019-05-28

### Fixed

- POM file missing `okhttp` library version.
  This was caused by [gradle/gradle#9565](https://github.com/gradle/gradle/issues/9565).

## [0.9.0] - 2019-05-22

### Changed

- Dependency version updates
- Updated default versions:

  | Component | From | To |
  |---|---|---|
  | Core | 2.138.3 | 2.164.3 |
  | Test Harness | 2.44 | 2.49 |
  | Workflow API Plugin | 2.33 | 2.34 |
  | Workflow Basic Steps Plugin | 2.13 | 2.16 |
  | Workflow CPS Plugin | 2.61 | 2.68 |
  | Workflow Global CPS Library Plugin | 2.12 | 2.13 |
  | Workflow Durable Task Step Plugin | 2.26 | 2.30 |
  | Workflow Multibranch Plugin | 2.20 | 2.21 |
  | Workflow Step API Plugin | 2.16 | 2.19 |
  | Workflow Job Plugin | 2.29 | 2.32 |
  | Workflow Support Plugin | 2.23 | 3.3 |

### Fixed

- The header key for both API token authentication and basic authentication is now `Authorization` instead of the incorrect `Authentication`

## [0.8.0] - 2018-11-30

> [!NOTE]
> This version requires at least Gradle 5.0.

### Changed

- A few unintentional public functions have been moved to `internal`
- Tasks created and configured in plugins have been updated to use configuration avoidance (`named`, `register`) while configurations and other domain objects have been switched back to the immediate APIs (`create`, `getByName`)
- Dependency version updates
- Updated default versions:

  | Component | From | To |
  |---|---|---|
  | Core | 2.121.3 | 2.138.3 |
  | Test Harness | 2.40 | 2.44 |
  | Workflow API Plugin | 2.29 | 2.33 |
  | Workflow Basic Steps Plugin | 2.10 | 2.13 |
  | Workflow CPS Plugin | 2.54 | 2.61 |
  | Workflow Global CPS Library Plugin | 2.10 | 2.12 |
  | Workflow Durable Task Step Plugin | 2.21 | 2.26 |
  | Workflow Job Plugin | 2.24 | 2.29 |
  | Workflow SCM Step Plugin | 2.6 | 2.7 |
  | Workflow Support Plugin | 2.20 | 2.23 |

## [0.7.0] - 2018-09-04

> [!NOTE]
> This version requires at least Gradle 4.10.

### Changed

- Built with Gradle 4.10 and Kotlin DSL Plugin 1.0-rc-3
- Updated default versions:

  | Component | From | To |
  |---|---|---|
  | Core | 2.107.2 | 2.121.3 |
  | Test Harness | 2.38 | 2.40 |
  | Workflow API Plugin | 2.26 | 2.29 |
  | Workflow Basic Steps Plugin | 2.6 | 2.10 |
  | Workflow CPS Plugin | 2.47 | 2.54 |
  | Workflow Durable Task Step Plugin | 2.19 | 2.21 |
  | Workflow Global CPS Library Plugin | 2.9 | 2.10 |
  | Workflow Job Plugin | 2.18 | 2.24 |
  | Workflow Multibranch Plugin | 2.17 | 2.20 |
  | Workflow Step API Plugin | 2.14 | 2.16 |
  | Workflow Support Plugin | 2.18 | 2.20 |

## [0.6.2] - 2018-04-12

### Changed

- Rebaseline core version to latest LTS due to [2018-04-11 security advisory](https://jenkins.io/security/advisory/2018-04-11):

  | Component | From | To |
  |---|---|---|
  | Core | 2.107.1 | 2.107.2 |

## [0.6.1] - 2018-04-11

### Fixed

- Error logs and problems with JEP-200 with latest Jenkins 2.107.1 LTS release
  ([JEP-200 announcement](https://jenkins.io/blog/2018/01/13/jep-200/), [LTS followup](https://jenkins.io/blog/2018/03/15/jep-200-lts))

## [0.6.0] - 2018-04-10

### Added

- All `org.jenkins-ci.modules` group dependencies from the `jenkins-war` dependency are included in integration tests.
  This should reduce error noise from Jenkins during tests.

### Changed

- `javapoet`, `okhttp`, and `kotlin-logging` package version upgrades
- **Breaking** — all exposed extension properties have been changed to the [`Property` API](https://docs.gradle.org/current/javadoc/org/gradle/api/provider/Property.html).
  In Groovy the existing DSL still works due to Gradle DSL decoration:

  ```groovy
  sharedLibrary {
    coreVersion = "2.114"
    testHarnessVersion = "2.32"
    pluginDependencies {
      workflowCpsPluginVersion = "2.4"
      workflowCpsGlobalLibraryPluginVersion = "2.9"
      dependency("io.jenkins.blueocean", "blueocean-web", "1.3.0")
    }
  }
  ```

  In the Kotlin DSL, use `.set` until [kotlin-dsl/380](https://github.com/gradle/kotlin-dsl/issues/380) is resolved:

  ```kotlin
  sharedLibrary {
    coreVersion.set("2.86")
    testHarnessVersion.set("2.32")
    pluginDependencies {
      workflowCpsGlobalLibraryPluginVersion.set("2.9")
      workflowCpsPluginVersion.set("2.4")
      dependency("io.jenkins.blueocean", "blueocean-web", "1.3.0")
    }
  }
  ```

- Updated default versions:

  | Component | From | To |
  |---|---|---|
  | Core | 2.89.4 | 2.107.1 |
  | Test Harness | 2.34 | 2.38 |
  | Workflow CPS Plugin | 2.45 | 2.47 |
  | Workflow Job Plugin | 2.17 | 2.18 |

## [0.5.0] - 2018-03-06

### Added

- New plugin to integrate with a specific Jenkins instance.
  Adds tasks to download the GDSL, retrieve plugin lists, and retrieve the core version.
  These tasks are experimental.

  ```groovy
  import java.net.URL
  import com.mkobit.jenkins.pipelines.http.BasicAuthentication

  jenkinsIntegration {
    baseUrl = new URL('https://mycorp.jenkins.zone')
    authentication = providers.provider { new BasicAuthentication(property('username'), property('password')) }
  }
  ```

  ```
  ./gradlew retrieveJenkinsGdsl
  ./gradlew retrieveJenkinsPluginData
  ./gradlew retrieveJenkinsVersion
  ```

- Support for using Jenkins core and plugins in library source code
- Support for `@Grab` in library source

  > [!WARNING]
  > Unit testing code that uses `@Grab` does not work.
  > See [this StackOverflow question](https://stackoverflow.com/questions/4611230/no-suitable-classloader-found-for-grab).
  > You can still test other code that does not use `@Grab`.

### Changed

- Updated default versions:

  | Component | From | To |
  |---|---|---|
  | Core | 2.89.2 | 2.89.4 |
  | Test Harness | 2.33 | 2.34 |
  | Workflow API Plugin | 2.24 | 2.26 |
  | Workflow CPS Plugin | 2.42 | 2.45 |
  | Workflow Durable Task Step Plugin | 2.17 | 2.19 |
  | Workflow Job Plugin | 2.16 | 2.17 |
  | Workflow Support Plugin | 2.16 | 2.18 |

### Fixed

- KDoc links to external documentation

### Removed

- Support for Gradle 4.3, 4.4, and 4.5 — only 4.6 is supported
- `integrationTest` source set configurations no longer extend from any `test` source set configurations — specify dependencies for both separately

## [0.4.0] - 2018-01-06

### Added

- Support for `@NonCPS` in library definition

### Changed

- Upgraded to Gradle 4.4.1
- Upgraded to Kotlin 1.2.10
- Updated default versions:

  | Component | From | To |
  |---|---|---|
  | Core | 2.73.2 | 2.89.2 |
  | Test Harness | 2.31 | 2.33 |
  | Workflow API Plugin | 2.22 | 2.24 |
  | Workflow CPS Plugin | 2.40 | 2.42 |
  | Workflow Durable Task Step Plugin | 2.15 | 2.17 |
  | Workflow Job Plugin | 2.14.1 | 2.16 |
  | Workflow Step API Plugin | 2.13 | 2.14 |
  | Workflow Support Plugin | 2.15 | 2.16 |

### Fixed

- Generated library retriever no longer logs on the same line as the first step

## [0.3.2] - 2017-10-31

### Fixed

- Constructor visibility in generated library retriever should be `public`

## [0.3.1] - 2017-10-31

### Fixed

- Build fails when run in a non-clean workspace

## [0.3.0] - 2017-10-31

> [!NOTE]
> Built and tested on Gradle 4.3.

### Added

- Generated classes for integration tests in the `com.mkobit.jenkins.pipelines.codegen` package namespace.
  The first generated class is `LocalLibraryRetriever`, which can be used as a `LibraryRetriever` for fast feedback in integration tests.

### Changed

- `integrationTest` will execute after `test` if both are included in the build
- `check` now `dependsOn` `integrationTest`
- Default Jenkins Test Harness version: `2.28` → `2.31`
- Default Jenkins Core version: `2.73.1` → `2.73.2`

### Removed

- Helper methods from `PluginDependencySpec` for adding dependencies from different groups: `cloudbees()`, `workflow()`, `jvnet()`, `jenkinsCi()`, and `blueocean()`
- `git-plugin` no longer included

## [0.2.0] - 2017-10-04

Fixes publishing issues with first release.

## [0.1.0] - 2017-09-11

Initial release.
