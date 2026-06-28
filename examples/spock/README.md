## Spock example

Demonstrates Spock 2.x as the integration-test framework for a Jenkins shared library, exercised against a real Jenkins instance via `JenkinsRule`.

### Known limitation — `sandbox=true` (issue #159)

Spock 2.x brings `groovy:3.x` onto the test classpath while Jenkins still bundles `groovy-all:2.4.21` in the WAR.
With `sandbox=true` the CPS transformer runs inside the test JVM and generates bytecode that the JVM rejects.
With `sandbox=false` the transformer is bypassed and tests run cleanly.

`SandboxLimitationPinSpec` pins the current failure mode.
When the constraint clears upstream (Jenkins drops `groovy-all`, or a Spock variant lands with Groovy 2.x compat) the pin will start failing — that is the signal to revisit issue #159 and enable `sandbox=true` here.

Use `sandbox=true` examples in the `junit-groovy`, `kotest`, and `basic` examples in the meantime; this example uses `sandbox=false` deliberately.
