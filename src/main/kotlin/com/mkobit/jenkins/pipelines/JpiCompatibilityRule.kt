package com.mkobit.jenkins.pipelines

import org.gradle.api.artifacts.CacheableRule
import org.gradle.api.attributes.AttributeCompatibilityRule
import org.gradle.api.attributes.CompatibilityCheckDetails

// When a consumer requests hpi/jpi artifacts but a component only publishes a jar
// (e.g., plain Java library transitives that appear alongside Jenkins plugins),
// accept the jar as compatible rather than failing resolution.
// Actual filtering to only .hpi files happens downstream via lenient artifact views.
@CacheableRule
class JpiCompatibilityRule : AttributeCompatibilityRule<String> {
  override fun execute(details: CompatibilityCheckDetails<String>) {
    val consumer = details.consumerValue ?: return
    val producer = details.producerValue ?: return
    if (consumer in HPI_TYPES && producer == JAR_TYPE) {
      details.compatible()
    }
  }

  companion object {
    private val HPI_TYPES = setOf("hpi", "jpi")
    private const val JAR_TYPE = "jar"
  }
}
