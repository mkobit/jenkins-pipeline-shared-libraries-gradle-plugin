package com.mkobit.jenkins.pipelines

import org.gradle.api.artifacts.CacheableRule
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule

/**
 * Restores `jakarta.servlet:jakarta.servlet-api` as a transitive dependency of
 * `jenkins-test-harness` for harness versions 2565 and later.
 *
 * Test-harness 2565+ excludes the servlet API from its published POM because the container
 * (Winstone) is expected to provide it at runtime. However the test JVM's classloader needs
 * to verify `JenkinsRule`'s bytecode before Winstone starts, so the API must be present on
 * the test classpath.
 *
 * Older harness versions (< 2565) already include the servlet API in their POM, so this
 * rule is a no-op for them.
 *
 * Applying this rule to all variants ensures the class is available at both
 * Groovy compile time (type-checking) and at test JVM class-loading time.
 */
@CacheableRule
internal abstract class JenkinsTestHarnessServletApiRule : ComponentMetadataRule {
  override fun execute(ctx: ComponentMetadataContext) {
    val leadingVersion = LEADING_INT.find(ctx.details.id.version)?.value?.toIntOrNull() ?: return
    if (leadingVersion < SERVLET_API_REQUIRED_FROM) return
    ctx.details.allVariants {
      withDependencies {
        add("jakarta.servlet:jakarta.servlet-api:${SharedLibraryDefaults.SERVLET_API_VERSION}")
      }
    }
  }

  companion object {
    // Harness 2565 removed jakarta.servlet-api from its published POM.
    private const val SERVLET_API_REQUIRED_FROM = 2565

    // Matches the leading integer in Jenkins version strings of any form:
    // "2565.vd1eb_7c961d1b_", "2565.1", "2565-rc1", "2565-beta.2", etc.
    private val LEADING_INT = Regex("""^\d+""")
  }
}
