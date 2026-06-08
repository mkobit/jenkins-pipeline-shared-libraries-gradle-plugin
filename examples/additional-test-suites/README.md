# Additional Test Suites Example

This example demonstrates how to configure and run custom test suites beyond the default `test` and `integrationTest` suites.

## Purpose
When writing a Jenkins pipeline shared library, you might want to separate different kinds of tests (e.g., unit tests, smoke tests, integration tests). This example shows how to use the Gradle Test Suites API in conjunction with the Jenkins pipeline shared library plugin to set up a new test suite (e.g., `smokeTest`).

It includes:
- Registering a new `JvmTestSuite`.
- Configuring the new test suite to use the Jenkins Test Harness.
- Organizing test sources appropriately.
