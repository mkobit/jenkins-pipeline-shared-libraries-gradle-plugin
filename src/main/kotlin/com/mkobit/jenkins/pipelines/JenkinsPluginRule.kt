package com.mkobit.jenkins.pipelines

import org.gradle.api.artifacts.CacheableRule
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

@CacheableRule
abstract class JenkinsPluginRule
  @Inject
  constructor(
    private val objects: ObjectFactory,
  ) : ComponentMetadataRule {
    override fun execute(ctx: ComponentMetadataContext) {
      val id = ctx.details.id
      if (!isJenkinsPluginGroup(id.group)) return

      ctx.details.withVariant("compile") {
        attributes {
          attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE)
          attribute(JENKINS_ARTIFACT_ATTRIBUTE, "jar")
        }
        withFiles {
          removeAllFiles()
          addFile("${id.name}-${id.version}.jar")
        }
      }

      // Separate java-runtime variant backed by the JAR so unit test runtimeClasspaths
      // resolve plugin classes without picking up the HPI runtime variant.
      ctx.details.addVariant("jar-runtime", "compile") {
        attributes {
          attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
          attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE)
          attribute(JENKINS_ARTIFACT_ATTRIBUTE, "jar")
        }
      }

      ctx.details.withVariant("runtime") {
        attributes {
          attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "hpi")
          attribute(JENKINS_ARTIFACT_ATTRIBUTE, "hpi")
        }
        withFiles {
          removeAllFiles()
          addFile("${id.name}-${id.version}.hpi")
        }
      }
    }

    companion object {
      val JENKINS_ARTIFACT_ATTRIBUTE: Attribute<String> =
        Attribute.of("com.mkobit.jenkins.artifact", String::class.java)

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
