package com.mkobit.jenkins.pipelines

data class PluginDependency(val group: String, val name: String, val version: String) {
  companion object {
    fun fromString(notation: String): PluginDependency {
      val splitNotation = notation.split(":")
      if (splitNotation.size != 3) {
        throw IllegalArgumentException("$notation can not be split into a dependency")
      }
      splitNotation
        .filter { it.isBlank() }
        .forEach { throw IllegalArgumentException("$notation must not have an empty part") }

      return PluginDependency(splitNotation[0], splitNotation[1], splitNotation[2])
    }
  }

  fun asStringNotation(): String = "$group:$name:$version"
}
