# Examples directory

Each subdirectory is a standalone Gradle project demonstrating a plugin usage pattern.
Run from the repo root with `./gradlew :examples:example-<name>`.
Examples do not need their own Gradle wrapper.

Use `basic-vars/` as the reference when adding a new example.

## Known workarounds

- **CodeNarc (#173):** disable `codenarcMain` and `codenarcTest` in each example until the issue ships a default config.
- **Groovy on unit test classpath (#161):** add `implementation("org.codehaus.groovy:groovy:2.4.21")` to the `test` suite until the plugin auto-adds it alongside JPU.
