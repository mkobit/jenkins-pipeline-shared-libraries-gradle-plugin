# Explicit library name example

This example demonstrates how to override the default shared library name and disable implicit loading.

## Purpose

By default, the plugin might infer the library name and assume it's implicitly loaded in integration tests.
This example shows how to use the `sharedLibrary` extension to explicitly set the `libraryName` and set `implicit = false`, which is useful for testing libraries that are explicitly loaded in pipelines using the `@Library` annotation or `library` step.
