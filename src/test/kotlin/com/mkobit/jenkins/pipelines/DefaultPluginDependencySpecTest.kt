package com.mkobit.jenkins.pipelines

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultPluginDependencySpecTest {

  private lateinit var pluginDependencySpec: DefaultPluginDependencySpec

  @BeforeEach
  internal fun setUp() {
    pluginDependencySpec = DefaultPluginDependencySpec()
  }

  @Test
  internal fun `cloudbees plugin`() {
    pluginDependencySpec.cloudbees("name", "1.0")

    assertThat(pluginDependencySpec.getDependencies()).hasOnlyOneElementSatisfying {
      assertThat(it.group).isEqualTo("com.cloudbees.jenkins.plugins")
      assertThat(it.name).isEqualTo("name")
      assertThat(it.version).isEqualTo("1.0")
    }
  }

  @Test
  internal fun `workflow plugin`() {
    pluginDependencySpec.workflow("name", "1.0")

    assertThat(pluginDependencySpec.getDependencies()).hasOnlyOneElementSatisfying {
      assertThat(it.group).isEqualTo("org.jenkins-ci.plugins.workflow")
      assertThat(it.name).isEqualTo("name")
      assertThat(it.version).isEqualTo("1.0")
    }
  }

  @Test
  internal fun `jvnet plugin`() {
    pluginDependencySpec.jvnet("name", "1.0")

    assertThat(pluginDependencySpec.getDependencies()).hasOnlyOneElementSatisfying {
      assertThat(it.group).isEqualTo("org.jvnet.hudson.plugins")
      assertThat(it.name).isEqualTo("name")
      assertThat(it.version).isEqualTo("1.0")
    }
  }

  @Test
  internal fun `jenkins-ci plugin`() {
    pluginDependencySpec.jenkinsCi("name", "1.0")

    assertThat(pluginDependencySpec.getDependencies()).hasOnlyOneElementSatisfying {
      assertThat(it.group).isEqualTo("org.jenkins-ci.plugins")
      assertThat(it.name).isEqualTo("name")
      assertThat(it.version).isEqualTo("1.0")
    }
  }

  @Test
  internal fun `blueocean plugin`() {
    pluginDependencySpec.blueocean("name", "1.0")

    assertThat(pluginDependencySpec.getDependencies()).hasOnlyOneElementSatisfying {
      assertThat(it.group).isEqualTo("io.jenkins.blueocean")
      assertThat(it.name).isEqualTo("name")
      assertThat(it.version).isEqualTo("1.0")
    }
  }

  @Test
  internal fun `explicit dependency plugin`() {
    pluginDependencySpec.dependency("com.mkobit.plugin","name", "1.0")

    assertThat(pluginDependencySpec.getDependencies()).hasOnlyOneElementSatisfying {
      assertThat(it.group).isEqualTo("com.mkobit.plugin")
      assertThat(it.name).isEqualTo("name")
      assertThat(it.version).isEqualTo("1.0")
    }
  }

  @Test
  internal fun `adding multiple plugins`() {
    pluginDependencySpec.cloudbees("cloudbees", "1.0")
    assertThat(pluginDependencySpec.getDependencies()).hasSize(1)
    pluginDependencySpec.workflow("workflow", "2.0")
    assertThat(pluginDependencySpec.getDependencies()).hasSize(2)
    pluginDependencySpec.jvnet("jvent", "3.0")
    assertThat(pluginDependencySpec.getDependencies()).hasSize(3)
    pluginDependencySpec.jenkinsCi("jenkinsCi", "4.0")
    assertThat(pluginDependencySpec.getDependencies()).hasSize(4)
    pluginDependencySpec.jenkinsCi("blueocean", "5.0")
    assertThat(pluginDependencySpec.getDependencies()).hasSize(5)
    pluginDependencySpec.dependency("com.mkobit", "mkobit-plugin", "6.0")
    assertThat(pluginDependencySpec.getDependencies()).hasSize(6)
  }
}
