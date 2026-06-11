# Peer-libraries composite example

This example demonstrates peer library dependencies across separate Gradle builds using composite build (`includeBuild`) and GAV notation.

## Purpose

The root project declares `library-a` and `library-b` as peers via their published coordinates.
`library-a` itself depends on `included-lib-3` for a helper step — that transitive peer is pulled in automatically because Gradle composes nested included builds and the `sharedLibrarySourceElements` variant propagates transitively.

## Dependency graph

```
peer-libraries-composite
├── library-a (com.example.pipeline:library-a:1.0)
│   └── included-lib-3 (com.example.pipeline:included-lib-3:1.0)
└── library-b (com.example.pipeline:library-b:1.0)
```

## Structure

- `library-a/` — provides `greetA`, calls `tag` from `included-lib-3`
- `library-b/` — provides `greetB`, standalone
- `included-lib-3/` — provides `tag`, a simple string-tagging helper
- `test/integration/java/GreetStepTest.java` — exercises both `greetA` (transitive) and `greetB`

## How the composite wiring works

Each library's `settings.gradle.kts` uses `pluginManagement { includeBuild("../../..") }` to find the plugin.
`library-a/settings.gradle.kts` also includes `included-lib-3` via `includeBuild("../included-lib-3")`.
The root only needs `includeBuild("library-a")` and `includeBuild("library-b")` — Gradle composes the nested included build automatically.
