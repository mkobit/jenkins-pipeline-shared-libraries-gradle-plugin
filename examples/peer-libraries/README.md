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

## `src/` class usage

`deploy-lib` and `shell-lib` each ship a class under `src/` (e.g. `DeployTarget`, `ShellStep`).
A library's own `vars/` scripts can import those classes freely because they share a classloader.
Cross-library class imports from a peer's `src/` do **not** work in the test harness — the correct pattern is to wrap the class in a `vars/` step and call that step from the root pipeline.

## Tests

Each library has unit tests (pipeline-unit) exercising its own vars in isolation.
The root `deploy-pipeline` library has:

- `test/unit` — mocks all four peer steps with `BasePipelineTest` to test orchestration logic without Jenkins
- `test/integration` — runs the full pipeline in an embedded Jenkins instance and asserts on log output
