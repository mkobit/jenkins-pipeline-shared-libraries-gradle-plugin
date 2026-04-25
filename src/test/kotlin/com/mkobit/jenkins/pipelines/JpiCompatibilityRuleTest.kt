package com.mkobit.jenkins.pipelines

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import org.gradle.api.attributes.AttributeCompatibilityRule
import org.gradle.api.attributes.CompatibilityCheckDetails

internal class JpiCompatibilityRuleTest :
  DescribeSpec({
    val rule: AttributeCompatibilityRule<String> = JpiCompatibilityRule()

    describe("HPI or JPI consumer with JAR producer") {
      withData(
        nameFn = { "marks compatible: consumer=$it, producer=jar" },
        "hpi",
        "jpi",
      ) { consumerValue ->
        val details = CompatibilityDetails(consumerValue, "jar")
        rule.execute(details)
        details.compatible.shouldBeTrue()
      }
    }

    describe("does not mark compatible for other combinations") {
      withData(
        nameFn = { (consumer, producer) -> "leaves undecided: consumer=$consumer, producer=$producer" },
        "jar" to "jar",
        "jar" to "hpi",
        "hpi" to "hpi",
      ) { (consumerValue, producerValue) ->
        val details = CompatibilityDetails(consumerValue, producerValue)
        rule.execute(details)
        details.compatible.shouldBeFalse()
      }
    }

    describe("null consumer or producer") {
      it("does nothing when consumer is null") {
        val details = CompatibilityDetails(null, "jar")
        rule.execute(details)
        details.compatible.shouldBeFalse()
      }

      it("does nothing when producer is null") {
        val details = CompatibilityDetails("hpi", null)
        rule.execute(details)
        details.compatible.shouldBeFalse()
      }
    }
  })

private class CompatibilityDetails(
  private val consumer: String?,
  private val producer: String?,
) : CompatibilityCheckDetails<String> {
  var compatible = false
    private set

  override fun getConsumerValue(): String? = consumer

  override fun getProducerValue(): String? = producer

  override fun compatible() {
    compatible = true
  }

  override fun incompatible() {}
}
