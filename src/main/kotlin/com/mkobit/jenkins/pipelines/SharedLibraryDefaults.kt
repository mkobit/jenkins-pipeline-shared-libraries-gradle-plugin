package com.mkobit.jenkins.pipelines

/**
 * Default versions shipped with the plugin, corresponding to the Jenkins 2.479.x LTS line.
 *
 * These are the values wired into every consumer project unless overridden via:
 *   sharedLibrary { jenkins { version = "..."; testHarnessVersion = "..." } }
 *
 * Unrelated to any versions a consumer declares in their own build for compilation or IDE support.
 */
object SharedLibraryDefaults {
  const val CORE_VERSION = "2.479.1"
  const val TEST_HARNESS_VERSION = "2391.v9b_3e2d3351a_2"
  const val BOM_VERSION = "5054.v620b_5d2b_d5e6"
  const val PIPELINE_UNIT_VERSION = "1.29"

  /** `groovy-all` version matching Jenkins 2.479.x's bundled Groovy runtime. Internal — subject to change once the Groovy 3 / 2.492.x strategy is resolved. */
  internal const val GROOVY_ALL_VERSION = "2.4.21"
  internal const val INTEGRATION_TEST_MAX_HEAP_SIZE = "2g"

  /**
   * Jakarta Servlet API version added as a `compileOnly`+runtime dependency of `jenkins-test-harness`
   * via [JenkinsTestHarnessServletApiRule]. Test-harness 2565+ excludes the API from its POM; the
   * test JVM needs it for class verification before Winstone starts. 5.0.0 is Jakarta EE 9 —
   * compatible with all current Jenkins LTS lines and sufficient for `JenkinsRule` bytecode verification.
   */
  internal const val SERVLET_API_VERSION = "5.0.0"
}
