# Plugin repo notes

## Gradle wrapper upgrade

When bumping the wrapper version in `gradle/wrapper/gradle-wrapper.properties`, add the new version to `gradleCompatVersions` in `src/ciMatrix/kotlin/com/mkobit/jenkins/pipelines/ci/MatrixCli.kt`.
That list is the single source of truth: it drives both the CI matrix JSON and `TestedGradleVersion.all` in functional tests.
CI workflows do not need editing — `functionalTestCurrentWrapper` and `-Ptest.gradle.version=current` both resolve to `GradleVersion.current()` at build time.
