# Examples directory

Each subdirectory is a standalone Gradle project demonstrating a plugin usage pattern.
Run from the repo root with `./gradlew :examples:example-<name>`.
Examples do not need their own Gradle wrapper.

Use [basic/](basic/) as the reference when adding a new example.

## Adding a new example

CI tasks and the dependency submission job auto-discover examples via Gradle — no workflow changes needed.
Dependabot does not auto-discover; add the new directory to the `directories` list in `.github/dependabot.yml` manually.
