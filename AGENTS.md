# Plugin repo notes

## Gradle wrapper upgrade

When bumping the wrapper version in `gradle/wrapper/gradle-wrapper.properties`, add the new version to `gradleCompatVersions` in `build-logic/src/main/kotlin/ci-tasks.gradle.kts`.
That list is the single source of truth: it drives the CI matrix JSON.
