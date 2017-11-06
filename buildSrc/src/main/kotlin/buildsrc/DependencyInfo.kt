package buildsrc

object DependencyInfo {
  val junitGradlePluginVersion: String = "1.0.1"
  val junitPlatformVersion: String = "1.0.1" 
  val junitPlatformGradlePluginVersion: String = "1.0.1" 
  val junitJupiterVersion: String = "5.0.1" 
  val junitVintageVersion: String = "4.12.1" 
  val junit5Log4jVersion: String = "2.9.1"

  val junitPlatformGradlePlugin = mapOf("group" to "org.junit.platform", "name" to "junit-platform-gradle-plugin", "version" to junitGradlePluginVersion)
  val junitPlatformRunner = mapOf("group" to "org.junit.platform", "name" to "junit-platform-runner", "version" to junitPlatformVersion)
  val junitJupiterApi = mapOf("group" to "org.junit.jupiter", "name" to "junit-jupiter-api", "version" to junitJupiterVersion)
  val junitJupiterParams = mapOf("group" to "org.junit.jupiter", "name" to "junit-jupiter-params", "version" to junitJupiterVersion)

  val junitTestImplementationArtifacts = listOf(
      junitPlatformRunner,
      junitJupiterApi,
      junitJupiterParams
  )

  val junitJupiterEngine = mapOf("group" to "org.junit.jupiter", "name" to "junit-jupiter-engine", "version" to junitJupiterVersion)
  val log4jCore = mapOf("group" to "org.apache.logging.log4j", "name" to "log4j-core", "version" to junit5Log4jVersion)
  val log4jJul = mapOf("group" to "org.apache.logging.log4j", "name" to "log4j-jul", "version" to junit5Log4jVersion)

  val junitTestRuntimeOnlyArtifacts = listOf(
      junitJupiterEngine,
      log4jCore,
      log4jJul
  )
}
