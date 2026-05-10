package com.mkobit.jenkins.pipelines.ci

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

/**
 * Jenkins LTS versions under test.
 * Java version is always 17 — Java compat variation is handled by the java-compat CI job.
 *
 * Maintenance:
 * - [Jenkins LTS & Releases](https://www.jenkins.io/changelog-stable/)
 * - [Jenkins BOM](https://github.com/jenkinsci/bom/releases)
 */
val jenkinsCompatEntries: List<JenkinsCompatEntry> =
  listOf(
    JenkinsCompatEntry(
      java = 17,
      jenkinsLts = "2.479.x",
      jenkinsVersion = "2.479.1",
      jenkinsBomVersion = "5054.v620b_5d2b_d5e6",
    ),
    JenkinsCompatEntry(
      java = 17,
      jenkinsLts = "2.528.x",
      jenkinsVersion = "2.528.3",
      jenkinsBomVersion = "6398.v1d26a_dd495e2",
    ),
    JenkinsCompatEntry(
      java = 17,
      jenkinsLts = "2.541.x",
      jenkinsVersion = "2.541.3",
      jenkinsBomVersion = "6364.v16b_76a_4023c7",
    ),
  )

internal val jenkinsCompatMatrix = CiMatrix(jenkinsCompatEntries)

/**
 * Gradle versions for gradle-compat CI.
 * See [Gradle Releases](https://gradle.org/releases/)
 */
val gradleCompatVersions = setOf("9.0.0", "9.1.0", "9.2.1", "9.3.1", "9.4.1", "9.5.0")

private val javaCompatVersions = setOf(21, 25)

fun main(args: Array<String>) {
  require(args.size == 2) { "Usage: MatrixCli <subcommand> <output-file>" }
  val (subcommand, outPath) = args
  val outFile = Path.of(outPath).also { it.parent?.createDirectories() }
  when (subcommand) {
    "jenkins" -> {
      outFile.writeText(jenkinsCompatMatrix.toJson())
    }

    "gradle" -> {
      outFile.writeText(
        CiMatrix(gradleCompatVersions.map { v -> GradleCompatEntry(v) }).toJson(),
      )
    }

    "java-compat" -> {
      outFile.writeText(CiMatrix(javaCompatVersions.map { JavaCompatEntry(it) }).toJson())
    }

    else -> {
      error("Unknown subcommand: $subcommand (expected: jenkins, gradle, java-compat)")
    }
  }
}
