package com.mkobit.jenkins.pipelines

import org.gradle.api.artifacts.CacheableRule
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import org.gradle.api.artifacts.maven.PomModuleDescriptor
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.named
import javax.inject.Inject

@CacheableRule
internal abstract class JenkinsPluginRule
  @Inject
  constructor(
    private val objects: ObjectFactory,
  ) : ComponentMetadataRule {
    override fun execute(ctx: ComponentMetadataContext) {
      val pom = ctx.getDescriptor(PomModuleDescriptor::class.java) ?: return
      if (pom.packaging !in PLUGIN_PACKAGINGS) return

      val id = ctx.details.id
      val packaging = pom.packaging

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

      // Separate java-runtime variant backed by the JAR — unit test runtimeClasspaths
      // resolve plugin classes without picking up the HPI/JPI runtime variant.
      ctx.details.addVariant("jar-runtime", "compile") {
        attributes {
          attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>(Usage.JAVA_RUNTIME))
          attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE)
          attribute(JENKINS_ARTIFACT_ATTRIBUTE, "jar")
        }
      }

      ctx.details.withVariant("runtime") {
        attributes {
          attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, packaging)
          attribute(JENKINS_ARTIFACT_ATTRIBUTE, packaging)
        }
        withFiles {
          removeAllFiles()
          addFile("${id.name}-${id.version}.$packaging")
        }
        // Dependencies whose artifact selector has no classifier point at JARs and must be
        // stripped from the JPI/HPI runtime variant to avoid resolution conflicts when the
        // consumer requests HPI artifacts (mirrors JpiVariantRule in gradle-jpi-plugin).
        withDependencies {
          removeIf { dep -> dep.artifactSelectors.any { it.classifier.isNullOrEmpty() } }
        }
      }
    }

    companion object {
      private val PLUGIN_PACKAGINGS = setOf("hpi", "jpi")

      val JENKINS_ARTIFACT_ATTRIBUTE: Attribute<String> =
        Attribute.of("com.mkobit.jenkins.artifact", String::class.java)
    }
  }
