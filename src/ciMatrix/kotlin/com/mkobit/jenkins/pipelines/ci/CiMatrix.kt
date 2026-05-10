package com.mkobit.jenkins.pipelines.ci

/**
 * Typed CI matrix entry for Jenkins compat jobs in build.yml.
 * JSON keys use hyphen/underscore naming to match GitHub Actions matrix variable names.
 * Consumers such as functionalTest inspect fields to pin dependency versions per LTS line.
 */
data class JenkinsCompatEntry(
  val java: Int,
  val jenkinsLts: String,
  val jenkinsVersion: String,
  val jenkinsBomVersion: String,
)

internal data class GradleCompatEntry(
  val gradle: String,
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
          )
        },
    ),
  )

@JvmName("javaCompatToJson")
internal fun CiMatrix<JavaCompatEntry>.toJson(): String = encodeJson(mapOf("include" to include.map { e -> mapOf("java" to e.java) }))

/**
 * Hand-rolled rather than kotlinx.serialization: ciMatrix runs against the
 * Gradle-embedded Kotlin stdlib, so an external serialization library would
 * require resolving a version-matched artifact against that stdlib.
 */
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
