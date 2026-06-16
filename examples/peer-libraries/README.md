# Peer-libraries example

Demonstrates the peer-library DSL surface in a single build: subproject `project()` deps, an `includeBuild` GAV dep, transitive resolution, custom `libraryName` overrides, cross-library `src/` imports, and both unit and integration testing patterns.

## Libraries

| Library | Gradle dep type | Jenkins library name | Step |
|---|---|---|---|
| `deploy-lib` | `project(":deploy-lib")` | `deployer` | `deployTo(env, service)` |
| `shell-lib` | transitive via `deploy-lib` | `shell-utils` | `runShell(cmd)` |
| `checks-lib` | `project(":checks-lib")` | `pre-checks` | `preCheck(service)` |
| `notify-lib` | GAV via `includeBuild` | `notifier` | `notifySlack(msg)` |

`shell-lib` is only declared by `deploy-lib`; the root picks it up transitively through the `sharedLibrarySourceElements` variant chain.

## Dependency graph

```mermaid
graph TD
    DP[deploy-pipeline<br/>consumer]
    DL[deploy-lib<br/>deployer]
    CL[checks-lib<br/>pre-checks]
    NL[notify-lib<br/>notifier<br/>GAV/includeBuild]
    SL[shell-lib<br/>shell-utils]

    DP --> DL
    DP --> CL
    DP --> NL
    DL --> SL
```

## Cross-library `src/` imports

`deploy-lib` and `shell-lib` each ship classes under `src/`.
A library's vars or src files can import classes from another peer library directly — Jenkins gives every library in a pipeline run the same Groovy classloader, so cross-library type references resolve at runtime without any `@Library` annotation or merged sources.
The plugin wires peer `src/` onto `compileClasspath` so the same imports also compile cleanly under Gradle.

Demonstrated by:
- `deploy-lib/vars/crossImport.groovy` and `deploy-lib/src/com/example/CrossImportSrc.groovy` — both import `com.example.ShellStep` from `shell-lib` with a plain `import`.
- `deploy-pipeline/test/integration/java/CrossLibrarySrcImportTest.java` — runs them inside embedded Jenkins.

The one shape that doesn't work this way is **bidirectional** src/ references (lib A imports lib B *and* lib B imports lib A).
Jenkins would resolve them fine at runtime, but Gradle refuses to schedule the compile graph because A:jar would need to come before B:compileGroovy and vice versa.
The classic workaround is to merge both libraries' sources into one library's directory before compile.

## Tests

Each library has unit tests (pipeline-unit) exercising its own vars in isolation.
The `deploy-pipeline` consumer has:

- `test/unit` — `RunDeployTest.groovy` mocks all four peer steps with `BasePipelineTest` to test orchestration logic without Jenkins.
- `test/unit` — `RunDeployJpuPeerTest.groovy` loads peer libraries *for real* via JPU's `projectSource` retriever, reading peer locations from the `test.library.N.{name,location,implicit}` JVM properties the plugin injects on every test suite.
- `test/integration` — `RunDeployTest.java` runs the full pipeline in embedded Jenkins. `CrossLibrarySrcImportTest.java` covers cross-library `src/` class visibility.
