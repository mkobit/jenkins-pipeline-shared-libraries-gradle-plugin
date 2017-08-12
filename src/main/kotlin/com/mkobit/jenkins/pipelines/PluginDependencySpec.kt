package com.mkobit.jenkins.pipelines

interface PluginDependencySpec {

  /**
   * Helper for adding plugins from the `com.cloudbees.jenkins.plugins` group.
   */
  fun cloudbees(name: String, version: String) = dependency("com.cloudbees.jenkins.plugins", name, version)

  /**
   * Helper for adding plugins from the `org.jenkins-ci.plugins.workflow` group.
   */
  fun workflow(name: String, version: String) = dependency("org.jenkins-ci.plugins.workflow", name, version)

  /**
   * Helper for adding plugins from the `org.jvnet.hudson.plugins` group.
   */
  fun jvnet(name: String, version: String) = dependency("org.jvnet.hudson.plugins", name, version)

  /**
   * Helper for adding plugins from the `org.jenkins-ci.plugins` group.
   */
  fun jenkinsCi(name: String, version: String) = dependency("org.jenkins-ci.plugins", name, version)

  /**
   * Helper for adding plugins from the `io.jenkins.blueocean` group.
   */
  fun blueocean(name: String, version: String) = dependency("io.jenkins.blueocean", name, version)

  /**
   * Adds a jenkinsCi dependency with the specified
   */
  fun dependency(group: String, name: String, version: String)

  fun getDependencies(): List<PluginDependency>
}

