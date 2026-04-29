package com.mkobit.jenkins.pipelines

import org.gradle.api.attributes.AttributeDisambiguationRule
import org.gradle.api.attributes.MultipleCandidatesDetails

// When multiple candidates are available and the consumer has not expressed a preference,
// prefer JAR over HPI so unit test classpaths use compiled classes rather than plugin archives.
internal class JenkinsArtifactDisambiguationRule : AttributeDisambiguationRule<String> {
  override fun execute(details: MultipleCandidatesDetails<String>) {
    if (details.consumerValue != null) return
    if ("jar" in details.candidateValues) {
      details.closestMatch("jar")
    }
  }
}
