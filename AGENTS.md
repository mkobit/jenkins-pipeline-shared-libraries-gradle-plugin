# Plugin repo notes

## Gradle wrapper upgrade

When bumping the wrapper version in `gradle/wrapper/gradle-wrapper.properties`, update `gradleCompatVersions` in **both** `build.gradle.kts` and `src/ciMatrix/kotlin/com/mkobit/jenkins/pipelines/ci/MatrixCli.kt`.
The list is intentionally duplicated: the task fan-out in `build.gradle.kts` runs at Gradle configuration time, before the `ciMatrix` source set has been compiled.
CI workflows do not need editing — `functionalTestCurrentWrapper` and `-Ptest.gradle.version=current` both resolve to `GradleVersion.current()` at build time.
