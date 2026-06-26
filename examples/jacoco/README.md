# JaCoCo example

Demonstrates how to use the JaCoCo plugin with a shared library to generate coverage reports for `src/` classes.

## Why vars are excluded

> [!NOTE]
> JenkinsPipelineUnit compiles `vars/` scripts at runtime via `GroovyScriptEngine`, producing a different class ID than the version JaCoCo instrumented at build time.
> Execution data for those classes will not match and coverage will be zero.

> [!NOTE]
> In real Jenkins, `vars/` scripts are also CPS-transformed by the Workflow CPS plugin, making the executed bytecode structurally different from what Gradle compiled.

Excluding default-package classes from `classDirectories` drops all `vars/` scripts from the report without naming individual files, leaving only `src/` classes with accurate coverage.

## Integration tests

The `integrationTest` suite also runs (against the embedded `jenkins-test-harness`) and exercises `vars/analyzeStatus.groovy` through real CPS compilation.
It is intentionally **not** wired into `jacocoTestReport`: CPS-transformed bytecode would produce the same class-ID mismatch as the unit-test case, and the integration runs would not contribute usable coverage data.

## Running

```sh
./gradlew :examples:example-jacoco
```

The report is generated at `examples/jacoco/build/reports/jacoco/test/html/index.html`.
