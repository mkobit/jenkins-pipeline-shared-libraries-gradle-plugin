# Peer-libraries example

This example demonstrates how to declare another shared library as a peer dependency using `sharedLibrary { dependencies { sharedLibrary(project(":peer-lib")) } }`.

## Purpose

The root project (`peer-libraries`) provides a `sayHello` step that delegates to the `greet` step defined in the `:peer-lib` subproject.
Both libraries are registered in the embedded Jenkins at integration-test time by the auto-registrar, so cross-library step calls work without any manual `GlobalLibraries` wiring.

## Structure

- `peer-lib/` — a minimal shared library that provides a `greet` step
- `vars/sayHello.groovy` — calls `greet(name)` from the peer library
- `test/integration/java/SayHelloStepTest.java` — verifies the cross-library call in an embedded Jenkins
