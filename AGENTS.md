# Plugin repo notes

## Gradle wrapper upgrade

When bumping the wrapper version in `gradle/wrapper/gradle-wrapper.properties`, add the new version to `gradleCompatVersions` in `build-logic/src/main/kotlin/ci-tasks.gradle.kts`.
That list is the single source of truth: it drives both the CI matrix JSON and `TestedGradleVersion.all` in functional tests (injected via the `test.gradle.versions` system property).
CI workflows do not need editing — `functionalTestCurrentWrapper` and `-Ptest.gradle.version=current` both resolve to `GradleVersion.current()` at build time.
