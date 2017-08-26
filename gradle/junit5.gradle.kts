val junitPlatformVersion: String by extra { "1.0.0-RC3" }
val junitPlatformGradlePluginVersion: String by extra { "1.0.0-RC3" }
val junitJupiterVersion: String by extra { "5.0.0-RC3" }
val junitVintageVersion: String by extra { "4.12.0-RC3" }
val junit5Log4jVersion: String by extra { "2.8.2" }

extra["junitTestImplementationArtifacts"] = mapOf(
  "junit-platform-runner" to mapOf("group" to "org.junit.platform", "name" to "junit-platform-runner", "version" to "${extra["junitPlatformVersion"]}"),
  "junit-jupiter-api" to mapOf("group" to "org.junit.jupiter", "name" to "junit-jupiter-api", "version" to "${extra["junitJupiterVersion"]}"),
  "junit-jupiter-params" to mapOf("group" to "org.junit.jupiter", "name" to "junit-jupiter-params", "version" to "${extra["junitJupiterVersion"]}")
)

extra["junitTestRuntimeOnlyArtifacts"] = mapOf(
  "junit-jupiter-engine" to mapOf("group" to "org.junit.jupiter", "name" to "junit-jupiter-engine", "version" to "${extra["junitJupiterVersion"]}"),
  "log4j-core" to mapOf("group" to "org.apache.logging.log4j", "name" to "log4j-core", "version" to "${extra["junit5Log4jVersion"]}"),
  "log4j-jul" to mapOf("group" to "org.apache.logging.log4j", "name" to "log4j-jul", "version" to "${extra["junit5Log4jVersion"]}")
)
