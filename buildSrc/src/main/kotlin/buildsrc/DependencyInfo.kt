package buildsrc

object DependencyInfo {
  const val okHttpVersion: String = "3.9.1"
  const val junitGradlePluginVersion: String = "1.0.3"
  const val junitPlatformVersion: String = "1.0.3"
  const val junitPlatformGradlePluginVersion: String = "1.0.3"
  const val junitJupiterVersion: String = "5.0.3"
  const val junitVintageVersion: String = "4.12.3"
  const val junit5Log4jVersion: String = "2.10.0"
  const val slf4jVersion: String = "1.7.25"

  const val okHttpClient = "com.squareup.okhttp3:okhttp:$okHttpVersion"
  const val okHttpMockServer = "com.squareup.okhttp3:mockwebserver:$okHttpVersion"

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
