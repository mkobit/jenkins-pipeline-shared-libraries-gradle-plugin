# Examples directory

Each subdirectory is a standalone Gradle project demonstrating a usage pattern for the shared-library plugin.

## Running examples

From the repo root: `./gradlew :examples:example-<name>`

The `:examples` project scans subdirectories and registers one `Exec` task per example.
Each example runs in its own Gradle daemon using the **root** `gradlew` — examples do not need their own wrapper.

## Creating a new example

Use `basic-vars/` as the starting-point template:

1. Create `examples/<name>/settings.gradle.kts` — copy from `basic-vars`, update `rootProject.name`
2. Create `examples/<name>/build.gradle.kts` — apply `id("com.mkobit.jenkins.pipelines.shared-library")`, add any extra plugins/deps the example demonstrates
3. Add shared library sources under `src/` and `vars/`
4. Add tests:
   - `test/unit/` — fast, no Jenkins runtime (Jenkins Pipeline Unit / JPU)
   - `test/integration/` — embedded Jenkins (full harness, `@WithJenkins`, `CpsFlowDefinition`)

## What the plugin auto-wires (no consumer config needed)

| Area | Detail |
|---|---|
| `test` suite | `useJUnitJupiter()`, sources at `test/unit/{java,groovy}`, Jenkins Pipeline Unit dep |
| `integrationTest` suite | Jenkins test harness, HPI files, WAR, `SharedLibraryAutoRegistrar`, sources at `test/integration/{java,groovy}` |
| `groovy-all` | excluded from all compile classpaths to prevent Groovy 2.4/3.x conflicts |
| integration task limits | `maxParallelForks = 1`, `maxHeapSize = "2g"` |
| `check` | depends on both `test` and `integrationTest` |

## Known workarounds

- **CodeNarc (#173):** `codenarcMain` and `codenarcTest` are enabled by default but no config ships yet — disable them in each example until the issue is resolved:
  ```kotlin
  tasks.named("codenarcMain") { enabled = false }
  tasks.named("codenarcTest") { enabled = false }
  ```
- **Groovy on unit test classpath (#161):** add `implementation("org.codehaus.groovy:groovy:2.4.21")` to the `test` suite until the plugin auto-adds it alongside JPU.
