package com.mkobit.jenkins.pipelines.ci

import java.io.File

// Jenkins LTS versions under test; each maps to a functionalTestJenkins* task in build.gradle.kts.
// Java version is always 17 — Java compat variation is handled by the java-compat CI job.
// JenkinsSessionFixture was introduced in harness 2554; the 2.479.x entry overrides the
// BOM-pinned 2391 so the base-class pattern in the example compiles.
val jenkinsCompatEntries: List<JenkinsCompatEntry> =
  listOf(
    JenkinsCompatEntry(
      java = 17,
      jenkinsLts = "2.479.x",
      jenkinsVersion = "2.479.1",
      jenkinsBomVersion = "5054.v620b_5d2b_d5e6",
      jenkinsTestHarness = "2554.v574c0503d196",
    ),
    JenkinsCompatEntry(
      java = 17,
      jenkinsLts = "2.528.x",
      jenkinsVersion = "2.528.3",
      jenkinsBomVersion = "6398.v1d26a_dd495e2",
      jenkinsTestHarness = "2565.vd1eb_7c961d1b_",
    ),
    JenkinsCompatEntry(
      java = 17,
      jenkinsLts = "2.541.x",
      jenkinsVersion = "2.541.3",
      jenkinsBomVersion = "6364.v16b_76a_4023c7",
      jenkinsTestHarness = "2565.vd1eb_7c961d1b_",
    ),
  )

internal val jenkinsCompatMatrix = CiMatrix(jenkinsCompatEntries)

// Keep in sync with gradleCompatVersions in build.gradle.kts (needed at configuration time).
val gradleCompatVersions = listOf("9.0.0", "9.1.0", "9.2.1", "9.3.1", "9.4.1", "9.5.0")

val javaCompatVersions = listOf(21, 25)

fun main(args: Array<String>) {
  require(args.size == 2) { "Usage: MatrixCli <subcommand> <output-file>" }
  val (subcommand, outPath) = args
  val outFile = File(outPath).also { it.parentFile?.mkdirs() }
  when (subcommand) {
    "jenkins" -> {
      outFile.writeText(jenkinsCompatMatrix.toJson())
    }

    "gradle" -> {
      outFile.writeText(
        CiMatrix(gradleCompatVersions.map { v -> GradleCompatEntry(v, v.replace(".", "_")) }).toJson(),
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
