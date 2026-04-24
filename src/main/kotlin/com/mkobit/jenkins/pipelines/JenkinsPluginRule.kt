package com.mkobit.jenkins.pipelines

import org.gradle.api.artifacts.CacheableRule
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import org.gradle.api.artifacts.type.ArtifactTypeDefinition

@CacheableRule
abstract class JenkinsPluginRule : ComponentMetadataRule {
  override fun execute(ctx: ComponentMetadataContext) {
    val id = ctx.details.id
    if (!isJenkinsPluginGroup(id.group)) return

    // Compile variant: use the published .jar so downstream compile classpaths resolve
    // the plugin's classes directly without extracting from the HPI archive.
    ctx.details.withVariant("compile") {
      attributes {
        attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE)
      }
      withFiles {
        removeAllFiles()
        addFile("${id.name}-${id.version}.jar")
      }
    }

    // Runtime variant: use the published .hpi so the embedded Jenkins runtime
    // (JenkinsRule) can install plugins during integration tests.
    ctx.details.withVariant("runtime") {
      attributes {
        attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "hpi")
      }
      withFiles {
        removeAllFiles()
        addFile("${id.name}-${id.version}.hpi")
      }
    }
  }

  companion object {
    internal fun isJenkinsPluginGroup(group: String): Boolean =
      group.startsWith("org.jenkins-ci.plugins") ||
        group.startsWith("org.jenkins-ci.modules") ||
        group.startsWith("io.jenkins.plugins") ||
        group.startsWith("org.jenkinsci.plugins") ||
        group.startsWith("io.jenkins") ||
        group.startsWith("org.6wind.jenkins") ||
        group.startsWith("com.cloudbees.jenkins.plugins") ||
        group.startsWith("com.cloudbees.plugins")
  }
}
