package com.mkobit.jenkins.pipelines.ci

/**
 * Typed CI matrix entries for build.yml (Gradle compat) and composite-test.yml (Jenkins
 * compat). JSON keys use hyphen/underscore naming to match GitHub Actions matrix variable
 * names; Kotlin properties use camelCase.
 *
 * [toJson] produces compact single-line JSON suitable for GitHub Actions `fromJSON()`
 * after being written to a file by MatrixCli.
 */

data class JenkinsCompatEntry(
  val java: Int,
  val jenkinsLts: String,
  val jenkinsVersion: String,
  val jenkinsBomVersion: String,
  val jenkinsTestHarness: String,
)

data class GradleCompatEntry(
  val gradle: String,
  val taskSuffix: String,
)

data class CiMatrix<T>(
  val include: List<T>,
)

// ── JSON serialization ─────────────────────────────────────────────────────────
// @JvmName disambiguates the two overloads at the JVM level (generic type erasure).

@JvmName("jenkinsCompatToJson")
fun CiMatrix<JenkinsCompatEntry>.toJson(): String =
  encodeJson(
    mapOf(
      "include" to
        include.map { e ->
          mapOf(
            "java" to e.java,
            "jenkins-lts" to e.jenkinsLts,
            "jenkins-version" to e.jenkinsVersion,
            "jenkins-bom-version" to e.jenkinsBomVersion,
            "jenkins-test-harness" to e.jenkinsTestHarness,
          )
        },
    ),
  )

@JvmName("gradleCompatToJson")
fun CiMatrix<GradleCompatEntry>.toJson(): String =
  encodeJson(
    mapOf(
      "include" to
        include.map { e ->
          mapOf(
            "gradle" to e.gradle,
            "task_suffix" to e.taskSuffix,
          )
        },
    ),
  )

// ── Recursive JSON encoder ─────────────────────────────────────────────────────
// Operates on plain Kotlin types — no pre-encoded intermediates — to avoid
// double-escaping when nested structures are passed as values.

internal fun encodeJson(v: Any?): String =
  when (v) {
    is String -> {
      "\"${v.replace("\\", "\\\\").replace("\"", "\\\"")}\""
    }

    is Number -> {
      v.toString()
    }

    is Boolean -> {
      v.toString()
    }

    is Map<*, *> -> {
      v.entries.joinToString(",", "{", "}") { (k, value) ->
        "\"$k\":${encodeJson(value)}"
      }
    }

    is List<*> -> {
      v.joinToString(",", "[", "]") { encodeJson(it) }
    }

    null -> {
      "null"
    }

    else -> {
      "\"${v.toString().replace("\\", "\\\\").replace("\"", "\\\"")}\""
    }
  }
