package com.mkobit.jenkins.pipelines

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.gradle.api.attributes.AttributeDisambiguationRule
import org.gradle.api.attributes.MultipleCandidatesDetails

internal class JenkinsArtifactDisambiguationRuleTest :
  DescribeSpec({
    val rule: AttributeDisambiguationRule<String> = JenkinsArtifactDisambiguationRule()

    describe("no consumer preference") {
      it("selects jar when jar and hpi are both candidates") {
        val details = DisambiguationDetails(null, setOf("jar", "hpi"))
        rule.execute(details)
        details.closestMatch shouldBe "jar"
      }

      it("selects jar when only jar is a candidate") {
        val details = DisambiguationDetails(null, setOf("jar"))
        rule.execute(details)
        details.closestMatch shouldBe "jar"
      }

      it("does not select anything when jar is not a candidate") {
        val details = DisambiguationDetails(null, setOf("hpi"))
        rule.execute(details)
        details.closestMatch.shouldBeNull()
      }
    }

    describe("consumer has a preference") {
      it("does not override an explicit hpi preference") {
        val details = DisambiguationDetails("hpi", setOf("jar", "hpi"))
        rule.execute(details)
        details.closestMatch.shouldBeNull()
      }

      it("does not override an explicit jar preference") {
        val details = DisambiguationDetails("jar", setOf("jar", "hpi"))
        rule.execute(details)
        details.closestMatch.shouldBeNull()
      }
    }
  })

private class DisambiguationDetails(
  private val consumer: String?,
  private val candidates: Set<String>,
) : MultipleCandidatesDetails<String> {
  var closestMatch: String? = null
    private set

  override fun getConsumerValue(): String? = consumer

  override fun getCandidateValues(): Set<String> = candidates

  override fun closestMatch(candidate: String) {
    closestMatch = candidate
  }
}
