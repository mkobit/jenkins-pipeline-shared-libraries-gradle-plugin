# Releasing

## One-time setup

Get an API key from [plugins.gradle.org](https://plugins.gradle.org/user/profile) under API Keys.
Add `GRADLE_PUBLISH_KEY` and `GRADLE_PUBLISH_SECRET` as repository secrets in Settings → Secrets → Actions.

## Normal release

Merge PRs to `main` using conventional commit prefixes (`feat:`, `fix:`, `deps:`, etc.).
After each merge, Release Please automatically opens or updates a `chore: release X.Y.Z` PR that bumps `gradle.properties` and updates `CHANGELOG.md`.
Edit the CHANGELOG entry in that PR to customise the release notes before merging.
Merging the release PR triggers the Release workflow, which publishes to the Gradle Plugin Portal, creates a git tag, and creates a GitHub Release with the built JARs attached.

## Version bump rules

| Prefix | Bump |
|---|---|
| `feat:` | minor |
| `fix:`, `perf:` | patch |
| `BREAKING CHANGE` in commit footer | major (minor while pre-1.0 — see note) |
| `deps:`, `chore:`, `ci:` | patch |

> [!NOTE]
> `release-please-config.json` sets `bump-minor-pre-major: true`, so while the released version is `0.x.y`, breaking changes bump the minor segment (e.g., `0.10.1` → `0.11.0`) rather than the major.
> Once the project releases `1.0.0`, breaking changes bump major.

## Manual re-trigger

If the Release workflow fails after a release PR is merged, re-run it from the Actions tab → Release → Run workflow.
Leave the version field blank to read from `gradle.properties`, or supply it explicitly to verify it matches.
