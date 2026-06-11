# Examples directory

Each subdirectory is a standalone Gradle project demonstrating a plugin usage pattern.

## Running examples

Always run from the **repo root**:

```
./gradlew :examples:example-<name>
```

Examples do not have their own Gradle wrapper — never `cd` into an example directory and invoke Gradle from there.

Use [basic/](basic/) as the reference when adding a new example.

## Adding a new example

CI tasks and the dependency submission job auto-discover examples via Gradle — no workflow changes needed.
Dependabot does not auto-discover; add the new directory to the `directories` list in `.github/dependabot.yml` manually.
