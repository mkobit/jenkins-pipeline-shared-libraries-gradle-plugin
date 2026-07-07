# Examples

This directory contains standalone Gradle builds demonstrating common usage patterns of the plugin.

## Running examples

The repository uses a Gradle composite build configuration to include all example projects.
You must run all Gradle tasks from the repository root using the root Gradle wrapper.
Do not invoke Gradle from within the individual example directories (e.g., do not `cd` into an example and run `./gradlew`).

### Running individual tasks

You can run individual tasks of any example build using the `:<example-name>:<task-name>` syntax.
For example, to run the `test` or `integrationTest` task for the `basic` example, run:

```shell
./gradlew :basic:test
./gradlew :basic:integrationTest
```

### Running full checks with memory constraints

The root project provides runner tasks in the `:examples` project named `:examples:example-<name>`.
These tasks run the full `check` suite for an example in a separate, memory-capped process.
For example, to run the full check for the `basic` example using this runner task, run:

```shell
./gradlew :examples:example-basic
```

To run checks for all examples in the repository, run:

```shell
./gradlew :examples:check
```

## Importing into IntelliJ IDEA

The main project and all examples load together in a single IntelliJ IDEA import.
Open or import the root project directory `jenkins-pipeline-shared-libraries-gradle-plugin` in IntelliJ IDEA.
The IDE automatically detects the composite build and configures all examples as included builds.
In the IDE's Gradle tool window, each example appears as a separate Gradle project under the main build.
You can run and debug tasks (like `check`, `test`, or `integrationTest`) directly from each example's task list in the tool window.
You can also run or debug individual unit and integration tests directly from the code editor using the built-in test runner.

## Troubleshooting

### Standalone import fails

The examples do not have their own Gradle wrappers or wrapper properties.
Never open or import an example directory directly as a standalone project.
Always open the root project instead.

### Missing examples in the IDE

If you add a new example directory or it does not appear in the IDE, trigger a Gradle sync.
This runs the auto-discovery logic and imports the new example as an included build.
