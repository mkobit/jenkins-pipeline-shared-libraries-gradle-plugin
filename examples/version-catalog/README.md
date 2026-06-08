# Version catalog example

This example demonstrates how to use a Gradle [version catalog](https://docs.gradle.org/current/userguide/platforms.html) to manage versions for the Jenkins pipeline shared library plugin and its dependencies.

## Purpose

Using a version catalog centralizes version declarations and makes dependency management easier across a project.
This example uses a `gradle/libs.versions.toml` file to manage versions for:
- Jenkins core and BOM
- JenkinsPipelineUnit
- Jenkins plugins required for tests

It shows how to reference these catalog dependencies in the `build.gradle.kts` to configure the `sharedLibrary` extension and Jenkins plugin dependencies.
