package com.mkobit.jenkins.pipelines

import org.gradle.api.artifacts.CacheableRule
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule

/**
 * Restores `jakarta.servlet:jakarta.servlet-api` as a transitive dependency of
 * `jenkins-test-harness`. Test-harness 2565+ excludes the servlet API from its
 * published POM because the container (Winstone) is expected to provide it.
 * However the test JVM's classloader needs to verify `JenkinsRule`'s bytecode
 * before Winstone starts, so the API must be present on the test classpath.
 *
 * Applying this rule to all variants ensures the class is available at both
 * Groovy compile time (type-checking) and at test JVM class-loading time.
 */
@CacheableRule
abstract class JenkinsTestHarnessServletApiRule : ComponentMetadataRule {
  override fun execute(ctx: ComponentMetadataContext) {
    ctx.details.allVariants {
      withDependencies {
        add("jakarta.servlet:jakarta.servlet-api:${SharedLibraryDefaults.SERVLET_API_VERSION}")
      }
    }
  }
}
