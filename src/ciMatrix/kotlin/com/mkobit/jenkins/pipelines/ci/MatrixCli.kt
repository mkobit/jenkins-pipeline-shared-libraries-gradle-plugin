package com.mkobit.jenkins.pipelines.ci

import java.io.File

// Gate entry: used by composite-test.yml directly, not part of the compat matrix.
val jenkinsGateEntry =
  JenkinsCompatEntry(
    java = 17,
    jenkinsLts = "2.528.x",
    jenkinsVersion = "2.528.3",
    jenkinsBomVersion = "6398.v1d26a_dd495e2",
    jenkinsTestHarness = "2565.vd1eb_7c961d1b_",
  )

internal val jenkinsCompatMatrix =
  CiMatrix(
    listOf(
      // Floor: minimum Java × minimum supported Jenkins LTS.
      // JenkinsSessionFixture was introduced in harness 2554; override the
      // BOM-pinned 2391 so the base-class pattern in the example compiles.
      JenkinsCompatEntry(
        java = 17,
        jenkinsLts = "2.479.x",
        jenkinsVersion = "2.479.1",
        jenkinsBomVersion = "5054.v620b_5d2b_d5e6",
        jenkinsTestHarness = "2554.v574c0503d196",
      ),
      // Latest LTS × maximum LTS Java.
      JenkinsCompatEntry(
        java = 21,
        jenkinsLts = "2.541.x",
        jenkinsVersion = "2.541.3",
        jenkinsBomVersion = "6364.v16b_76a_4023c7",
        jenkinsTestHarness = "2565.vd1eb_7c961d1b_",
      ),
      // Latest LTS × latest Java (forward-compat).
      JenkinsCompatEntry(
        java = 25,
        jenkinsLts = "2.541.x",
        jenkinsVersion = "2.541.3",
        jenkinsBomVersion = "6364.v16b_76a_4023c7",
        jenkinsTestHarness = "2565.vd1eb_7c961d1b_",
      ),
    ),
  )

// Keep in sync with gradleCompatVersions in build.gradle.kts (needed at configuration time).
val gradleCompatVersions = listOf("9.0.0", "9.1.0", "9.2.1", "9.3.1", "9.4.1")

val javaCompatVersions = listOf(21, 25)

fun main(args: Array<String>) {
  require(args.size == 2) { "Usage: MatrixCli <subcommand> <output-file>" }
  val (subcommand, outPath) = args
  val outFile = File(outPath).also { it.parentFile?.mkdirs() }
  when (subcommand) {
    "jenkins" -> {
      outFile.writeText(jenkinsCompatMatrix.toJson())
    }

    "jenkins-gate" -> {
      outFile.writeText(jenkinsGateEntry.toGateJson())
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
      error("Unknown subcommand: $subcommand (expected: jenkins, jenkins-gate, gradle, java-compat)")
    }
  }
}
