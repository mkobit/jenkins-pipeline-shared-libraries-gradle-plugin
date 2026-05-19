package com.mkobit.jenkins.pipelines

/**
 * Default versions shipped with the plugin, corresponding to the Jenkins 2.479.x LTS line.
 *
 * These are the values wired into every consumer project unless overridden via:
 *   sharedLibrary { jenkins { version = "..."; bomVersion = "..." } }
 *
 * Unrelated to any versions a consumer declares in their own build for compilation or IDE support.
 */
object SharedLibraryDefaults {
  const val CORE_VERSION = "2.479.1"
  const val BOM_VERSION = "5054.v620b_5d2b_d5e6"

  /**
   * Minimum `jenkins-test-harness` version wired into Jenkins test suites.
   * Not consumer-configurable — if you need a newer harness, declare it in your suite:
   * `dependencies { implementation("org.jenkins-ci.main:jenkins-test-harness:VERSION") }`
   * Gradle conflict resolution picks the highest requested version.
   */
  internal const val TEST_HARNESS_VERSION = "2565.vd1eb_7c961d1b_"
  const val PIPELINE_UNIT_VERSION = "1.29"

  /**
   * Full `groovy-all` coordinate matching Jenkins 2.479.x's bundled Groovy runtime.
   * Internal — subject to change once the Groovy 3 / 2.492.x strategy is resolved.
   */
  internal const val GROOVY_ALL_COORDINATES = "org.codehaus.groovy:groovy-all:2.4.21"
  internal const val IVY_COORDINATES = "org.apache.ivy:ivy:2.4.0"
internal const val INTEGRATION_TEST_MAX_HEAP_SIZE = "2g"

  /**
   * Jakarta Servlet API version added as a `compileOnly`+runtime dependency of `jenkins-test-harness`
   * via [JenkinsTestHarnessServletApiRule]. Test-harness 2565+ excludes the API from its POM; the
   * test JVM needs it for class verification before Winstone starts. 5.0.0 is Jakarta EE 9 —
   * compatible with all current Jenkins LTS lines and sufficient for `JenkinsRule` bytecode verification.
   */
  internal const val SERVLET_API_VERSION = "5.0.0"
}
