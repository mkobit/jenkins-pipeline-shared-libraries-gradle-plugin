# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.12.2](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/compare/v0.12.1...v0.12.2) (2026-06-30)


### Dependency updates

* **deps:** bump org.spockframework:spock-junit4 from 2.3-groovy-3.0 to 2.4-groovy-3.0 in /examples/spock ([#268](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/268)) ([d032766](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/commit/d03276644470c01e366895400f86080676846d95))

## [0.12.1](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/compare/v0.12.0...v0.12.1) (2026-06-29)


### Bug fixes

* restore configuration cache compatibility for plugin consumers ([#269](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/269)) ([8813b17](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/commit/8813b1755bf8a54d71038718d8e4904a0b75158e))

## [0.12.0](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/compare/v0.11.0...v0.12.0) (2026-06-28)


### Features

* add implicit DSL property to control @Library requirement ([#196](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/196)) ([b090e11](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/commit/b090e113b71a85d973a8062c48e5d5ef81bcd77b))
* Add jacoco example ([#231](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/231)) ([1da69e4](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/commit/1da69e4e0d042407172e8524591c73943fe462ad))
* peer shared library dependencies (issue [#158](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/158)) ([#164](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/164)) ([1a283c8](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/commit/1a283c85220b7623b77bfb0451599c0294b011af))
* per-suite jenkins.useTestHarness opt-in, JenkinsTestSuiteService slot, and example build serialization ([#224](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/224)) ([5707c16](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/commit/5707c168843761384967cdca0c362591d54d0cb4))


### Bug fixes

* replace @CacheableTask with @DisableCachingByDefault on ExtractJenkinsCodeNarcConfig ([#200](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/200)) ([7a1ce84](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/commit/7a1ce844b612cd84390944fd3811f871fcca9a62))
* replace groovy-all exclude with eachDependency substitution in test suite classpaths ([#161](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/161)) ([#201](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/201)) ([966c540](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/commit/966c540392a7656bf070c2434a7650733545922d))
* ship bundled default codenarc config for main and test source sets ([#199](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/199)) ([c793e1f](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/commit/c793e1f66ae4c7f28c006d135cd82fde97959d50))


### Dependency updates

* bump com.gradle.develocity to 4.4.2, ignore in example scopes ([#259](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/259)) ([ed7a507](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/commit/ed7a5073230b3bfbc22a3c296c3548be218fa2bb))
* **deps:** bump com.diffplug.spotless from 8.5.1 to 8.6.0 ([#249](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/249)) ([627d0a5](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/commit/627d0a5850ebba1d609fdda926cf5b95fc939522))
* **deps:** bump io.mockk:mockk from 1.14.9 to 1.14.11 in the testing group across 1 directory ([#244](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/244)) ([8f60926](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/commit/8f60926e16e5cfe980f417a424ef97f064976716))
* **deps:** bump jvm from 2.1.21 to 2.3.21 in /examples/kotest ([#225](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/225)) ([98cc32f](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/commit/98cc32fc55db95c7ffece7176a5f68171383694b))
* **deps:** bump jvm from 2.3.21 to 2.4.0 in /examples/kotest ([#258](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/258)) ([36e47d3](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/commit/36e47d3569d03f82d942ce5ca125e4ba47c85b54))
* **deps:** bump org.6wind.jenkins:lockable-resources from 1515.v380548282a_59 to 1524.v2c727b_b_e56ef in /examples/version-catalog ([#261](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/261)) ([2908343](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/commit/29083431bf6d576f8afdbef56c830120ee7a94b9))
* **deps:** bump org.jetbrains.kotlinx:kotlinx-coroutines-core from 1.10.1 to 1.11.0 in /examples/kotest in the jetbrains group across 1 directory ([#204](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/204)) ([717c9a3](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/commit/717c9a3ffe0c0f834eeb42f0850331e189988f10))
* **deps:** bump org.openrewrite.rewrite from 7.32.1 to 7.32.2 ([#169](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/169)) ([76da5df](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/commit/76da5dfcb97329ba0bb0c9385bc94a64c6a769cd))
* **deps:** bump org.openrewrite.rewrite from 7.32.2 to 7.33.0 ([#233](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/233)) ([66d1524](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/commit/66d15242387b5b4bbad4bc2617a48e6fe5cc0604))


### Documentation

* add additional test suites example ([#190](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/190)) ([45d3437](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/commit/45d3437a41c5001c98876abbf6274d1ba0a915b7))
* add basic-vars example with JPU and integration tests ([#172](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/172)) ([edcfc51](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/commit/edcfc511b101aa33bd3c7c0ce96955716327b19c))
* add codenarc example ([#188](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/188)) ([4e5ccba](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/commit/4e5ccbadf958f054df27c2160ad6209b6f88a4a6))
* add explicit library naming example ([#193](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/193)) ([4a72030](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/commit/4a72030ec92f35f4c0cf5e9458aaa5c72eb90320))
* add Jenkins Test Harness Kotest matchers to examples ([#191](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/191)) ([0183acc](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/commit/0183accfb11c720fad340f3a4c87af58459c8271))
* add kotest testing example ([#185](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/185)) ([97fb276](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/commit/97fb2769ab903299bd8dbf6837d6d59283e8832c))
* add libraryResource example ([#192](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/192)) ([49a7c2e](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/commit/49a7c2efee98b905f92a3187b2d7a54dc8829be9))
* add spock example with sandbox=true pinning test ([#267](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/267)) ([d63272c](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/commit/d63272c625a948dbf8b944f6a50cfa26defa32a4))
* add version catalog example ([#197](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/197)) ([b43a6ae](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/commit/b43a6ae4379ce786a386eef10107149077bf7535))
* clarify autoRegisterLibrary and implicit in README and extension docs ([#218](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/218)) ([db8da8c](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/commit/db8da8c2de55dda46de6f5e350875c689bac3862))
* remove stale HEAD-tracking note from README ([#156](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/156)) ([c29a08c](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/commit/c29a08ce5d2f00becd802b1766ac83822b69eaf5))
* update README to point to examples, reduce inline code noise ([#202](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/202)) ([7cc11ca](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/commit/7cc11cadc6669841dd895d0df147d7eb0573d3cd))

## [0.11.0](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/compare/v0.10.1...v0.11.0) (2026-05-19)

> [!IMPORTANT]
> **This is a complete rewrite** — the first release since 0.10.1 (July 2019), nearly seven years later.
> The plugin has been rebuilt from the ground up for Gradle 9.4+, Java 17/21, and modern Jenkins LTS lines.
> All 0.10.x APIs have been removed.
> See the [migration guide in the README](README.md#migration-from-010x).

### Added

- Single `jenkinsPlugin` configuration for all plugin dependencies — replaces the old multi-configuration model.
- Jenkins BOM auto-injection — no explicit `jenkinsPlugin(platform(...))` call is needed; the BOM coordinate is derived from `jenkins.version` (e.g., `2.479.1` → `bom-2.479.x`).
- `sharedLibrary.plugins { plugin("group:artifact") }` DSL block for declaring Jenkins plugin dependencies inline inside the extension; equivalent to `dependencies { jenkinsPlugin("...") }`.
- `sharedLibrary.withJenkins(suite)` for opt-in Jenkins test-harness wiring on additional `JvmTestSuite` registrations.
- `autoRegisterLibrary` property (default: `true`) — generates `SharedLibraryAutoRegistrar` and registers the library in embedded Jenkins at startup; no `GlobalLibraries.get().setLibraries(...)` call needed in tests.
- `libraryName` property (default: `project.name`) — controls the Jenkins library identifier used in `@Library("...")` pipeline scripts and `LocalLibraryRetriever.implicitLibrary()`.
- Generated `LocalLibraryRetriever` class — loads the local shared library in integration tests without network access.
- Built-in Jenkins CodeNarc rules (`codenarcJenkinsMain` task) — validates CPS-safety and Serializable type compliance on shared library sources.
- ~~OpenRewrite migration recipe `com.mkobit.jenkins.pipelines.MigrateSharedLibraryPlugin010To011` for automated 0.10.x → 0.11.x migration.~~

> [!WARNING]
> This recipe was untested and has been removed in newer versions.
> The recommended path is to manually migrate your code.

- Configuration cache support.
- Java 17, 21, and 25 toolchain support.
- Jenkins LTS 2.479.x, 2.528.x, and 2.541.x compatibility with full BOM alignment.
- `syncSharedLibrarySource` task — syncs `src/`, `vars/`, and `resources/` into `build/sharedLibrarySource/{libraryName}/` as a cacheable, incremental operation.
- `sharedLibrarySourceElements` outgoing variant — exposes the synced source directory as a Gradle variant with `Category` and `Usage` attributes for cross-project resolution.

### Changed

- Minimum Gradle version is now 9.4 (previously 4.x–8.x).
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

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven {
            name = "jenkins"
            url = uri("https://repo.jenkins-ci.org/public/")
        }
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
        version = "2.541.3"
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
