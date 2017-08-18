package com.mkobit.jenkins.pipelines

class DefaultPluginDependencySpec : PluginDependencySpec {

  private val dependencies: MutableList<PluginDependency> = mutableListOf()

  override fun dependency(group: String, name: String, version: String) {
    dependencies.add(PluginDependency(group, name, version))
  }

  override fun getDependencies(): List<PluginDependency> = dependencies.toList()
}
