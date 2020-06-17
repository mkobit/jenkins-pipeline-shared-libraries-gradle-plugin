package com.mkobit.jenkins.pipelines

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo

internal class PluginDependencyTest {
  @ParameterizedTest
  @ValueSource(strings = ["", "::", " : : ", "group", "group:name:", ":name:version", "group::version", "group:name:version:thing"])
  internal fun `throws exception when constructing from invalid dependency notation`(notation: String) {
    expectThrows<IllegalArgumentException> {
      PluginDependency.fromString(notation)
    }
  }

  @Test
  internal fun `parse a dependency notation`() {
    val group = "group"
    val name = "name"
    val version = "version"
    val pluginDependency = PluginDependency.fromString("$group:$name:$version")

    expectThat(pluginDependency.group).isEqualTo(group)
    expectThat(pluginDependency.name).isEqualTo(name)
    expectThat(pluginDependency.version).isEqualTo(version)
  }

  @Test
  internal fun `as string notation`() {
    val group = "group"
    val name = "name"
    val version = "version"
    val pluginDependency = PluginDependency(group, name, version)

    expectThat(pluginDependency.asStringNotation()).isEqualTo("$group:$name:$version")
  }
}
