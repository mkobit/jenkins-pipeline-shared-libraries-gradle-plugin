# JaCoCo Example

Demonstrates how to use the JaCoCo plugin with Jenkins Pipeline Shared Libraries to generate code coverage reports for unit tests.

## Why this is needed

Using `id("jacoco")` works easily with normal unit tests in Gradle. However, when working with Jenkins Pipeline code, the pipeline scripts are compiled dynamically at runtime, or class formats are altered, causing JaCoCo to report mismatch errors or missing execution data.

To resolve this, we configure the `jacocoTestReport` task to exclude dynamically interpreted pipeline code (like `analyzeStatus*`) from its `classDirectories`, allowing standard library code coverage to be accurately calculated.

## Relevant Issues

* [#32 Determine how to use `jacoco` plugin to get coverage for integration tests](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/32)
* [#230 Add example from starting JaCoCo usage within unit test example](https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/230)

## Running

Run the tests and generate a JaCoCo report:

```sh
./gradlew test jacocoTestReport
```

The report will be available in `build/reports/jacoco/test/html/index.html`. Integration tests will still run but may not produce meaningful jacoco metrics for dynamic pipeline code.
