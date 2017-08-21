package com.mkobit.jenkins.pipelines

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class PluginDependencyTest {
  @ParameterizedTest
  @ValueSource(strings = arrayOf("", "::", " : : ", "group", "group:name:", ":name:version", "group::version", "group:name:version:thing"))
  internal fun `throws exception when constructing from invalid dependency notation`(notation: String) {
    assertThatThrownBy { PluginDependency.fromString(notation) }.isInstanceOf(IllegalArgumentException::class.java)
  }

  @Test
  internal fun `parse a dependency notation`() {
    val group = "group"
    val name = "name"
    val version = "version"
    val pluginDependency = PluginDependency.fromString("$group:$name:$version")

    assertThat(pluginDependency.group).isEqualTo(group)
    assertThat(pluginDependency.name).isEqualTo(name)
    assertThat(pluginDependency.version).isEqualTo(version)
  }

  @Test
  internal fun `as string notation`() {
    val group = "group"
    val name = "name"
    val version = "version"
    val pluginDependency = PluginDependency(group, name, version)

    assertThat(pluginDependency.asStringNotation()).isEqualTo("$group:$name:$version")
  }
}
