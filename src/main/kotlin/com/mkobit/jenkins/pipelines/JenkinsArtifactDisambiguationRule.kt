package com.mkobit.jenkins.pipelines

import org.gradle.api.attributes.AttributeDisambiguationRule
import org.gradle.api.attributes.MultipleCandidatesDetails

/**
 * Prefers JAR over HPI when the consumer has not expressed an artifact-type preference.
 *
 * When multiple candidates are available and the consumer has not expressed a preference,
 * prefer `jar` over `hpi` so unit test classpaths use compiled classes rather than plugin archives.
 */
internal class JenkinsArtifactDisambiguationRule : AttributeDisambiguationRule<String> {
  override fun execute(details: MultipleCandidatesDetails<String>) {
    if (details.consumerValue != null) return
    if ("jar" in details.candidateValues) {
      details.closestMatch("jar")
    }
  }
}
