package com.mkobit.jenkins.pipelines.ci

// Typed CI matrix entries for build.yml (Gradle compat) and composite-test.yml (Jenkins compat).
// JSON keys use hyphen/underscore naming to match GitHub Actions matrix variable names.
// Public: JenkinsCompatEntry — consumers (e.g. functionalTest) may inspect fields.
// Internal: GradleCompatEntry, JavaCompatEntry, CiMatrix, toJson/toGateJson — serialization details.
data class JenkinsCompatEntry(
  val java: Int,
  val jenkinsLts: String,
  val jenkinsVersion: String,
  val jenkinsBomVersion: String,
  val jenkinsTestHarness: String,
)

// Internal: serialization shapes only, not part of the public registry API.
internal data class GradleCompatEntry(
  val gradle: String,
  val taskSuffix: String,
)

internal data class JavaCompatEntry(
  val java: Int,
)

internal data class CiMatrix<T>(
  val include: List<T>,
)

@JvmName("jenkinsCompatToJson")
internal fun CiMatrix<JenkinsCompatEntry>.toJson(): String =
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
internal fun CiMatrix<GradleCompatEntry>.toJson(): String =
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

@JvmName("javaCompatToJson")
internal fun CiMatrix<JavaCompatEntry>.toJson(): String = encodeJson(mapOf("include" to include.map { e -> mapOf("java" to e.java) }))

// Flat JSON object for a single gate entry — consumed by composite-test.yml via fromJSON.
internal fun JenkinsCompatEntry.toGateJson(): String =
  encodeJson(
    mapOf(
      "java" to java,
      "jenkins-lts" to jenkinsLts,
      "jenkins-version" to jenkinsVersion,
      "jenkins-bom-version" to jenkinsBomVersion,
      "jenkins-test-harness" to jenkinsTestHarness,
    ),
  )

// Hand-rolled rather than kotlinx.serialization: ciMatrix runs against the
// Gradle-embedded Kotlin stdlib, so an external serialization library would
// require resolving a version-matched artifact against that stdlib.

internal fun encodeJson(v: Any?): String =
  when (v) {
    is String -> {
      "\"${v.replace("""\""", """\\""").replace("\"", """\"""")}\""
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
