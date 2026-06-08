# Plugin repo notes

## Java version

The plugin and all examples compile against Java 17 (toolchain) and the test harness targets Java 17+.
When writing tests or examples, use modern Java features by default: text blocks (`"""`), `var`, records, switch expressions, etc.

## Gradle wrapper upgrade

When bumping the wrapper version in `gradle/wrapper/gradle-wrapper.properties`, add the new version to `gradleCompatVersions` in `build-logic/src/main/kotlin/ci-tasks.gradle.kts`.
That list is the single source of truth: it drives the CI matrix JSON.
