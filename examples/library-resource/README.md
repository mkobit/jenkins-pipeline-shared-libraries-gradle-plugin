# Library resource example

This example demonstrates how to test pipeline steps that read files using the `libraryResource` step.

## Purpose

Jenkins shared libraries can bundle static resources in the `resources/` directory.
These resources are accessed in pipelines using the `libraryResource` step.
This example shows how to structure the project to include these resources and how to write tests that verify steps utilizing `libraryResource`.
