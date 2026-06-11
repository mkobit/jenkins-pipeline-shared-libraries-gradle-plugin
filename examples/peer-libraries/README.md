# Peer-libraries example

Demonstrates all peer library dependency variants in a single build: subproject `project()` deps, an `includeBuild` GAV dep, transitive peer resolution, custom `libraryName` configuration, and both unit and integration testing patterns.

## Libraries

| Library | Gradle dep type | Jenkins library name | Step |
|---|---|---|---|
| `deploy-lib` | `project(":deploy-lib")` | `deployer` | `deployTo(env, service)` |
| `shell-lib` | transitive via `deploy-lib` | `shell-utils` | `runShell(cmd)` |
| `checks-lib` | `project(":checks-lib")` | `pre-checks` | `preCheck(service)` |
| `notify-lib` | GAV via `includeBuild` | `notifier` | `notifySlack(msg)` |

`shell-lib` is only declared as a peer of `deploy-lib`; the root picks it up transitively through the `sharedLibrarySourceElements` variant chain.

## `src/` class usage and classloader isolation

`deploy-lib` and `shell-lib` each ship a class under `src/` (`DeployTarget`, `ShellStep`).
A library's own `vars/` scripts can import those classes freely — they share the same `GroovyClassLoader`.

Cross-library class imports do **not** work at runtime.
Jenkins gives each loaded library its own `GroovyClassLoader`; these are siblings under the Jenkins system classloader, so one library's compiled `src/` classes are invisible to another library's scripts.
Adding `@Library` to the pipeline script does not bridge this gap — it only adds classes to the Jenkinsfile's classloader, not to other libraries' classloaders.
`vars/` steps are the only supported coupling point between libraries.

The Gradle plugin wires peer `src/` onto `compileClasspath` so cross-library imports compile cleanly; the failure is purely at runtime in the Jenkins classloader.

## Tests

Each library has unit tests (pipeline-unit) exercising its own vars in isolation.
The root `deploy-pipeline` library has:

- `test/unit` — mocks all four peer steps with `BasePipelineTest` to test orchestration logic without Jenkins
- `test/integration` — runs the full pipeline in an embedded Jenkins instance and asserts on log output
