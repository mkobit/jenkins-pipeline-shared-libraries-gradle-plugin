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

## Tests

Each library has unit tests (pipeline-unit) exercising its own vars in isolation.
The root `deploy-pipeline` library has:

- `test/unit` — mocks all four peer steps with `BasePipelineTest` to test orchestration logic without Jenkins
- `test/integration` — runs the full pipeline in an embedded Jenkins instance and asserts on log output
