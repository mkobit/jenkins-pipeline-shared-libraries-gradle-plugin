package com.mkobit.jenkins.pipelines

internal object PluginConstants {
  const val INTEGRATION_TEST_SUITE = "integrationTest"

  const val JENKINS_PLUGIN_CONFIGURATION = "jenkinsPlugin"
  const val JENKINS_PLUGIN_HPIS_CONFIGURATION = "jenkinsPluginHpis"
  const val JENKINS_WAR_CONFIGURATION = "jenkinsWar"

  const val GROOVY_ALL_RUNTIME_CONFIGURATION = "integrationTestGroovyAllRuntime"
  const val GROOVY_ALL_GROUP_AND_ARTIFACT = "org.codehaus.groovy:groovy-all"

  const val IVY_CONFIGURATION = "sharedLibraryIvy"
  const val IVY_COORDINATES = "org.apache.ivy:ivy:2.4.0"

  const val ANNOTATION_INDEXER_COORDINATES = "org.jenkins-ci:annotation-indexer:1.17"

  const val DEFAULT_PIPELINE_GROOVY_LIB = "io.jenkins.plugins:pipeline-groovy-lib"
  const val DEFAULT_WORKFLOW_JOB = "org.jenkins-ci.plugins.workflow:workflow-job"
  const val DEFAULT_WORKFLOW_BASIC_STEPS = "org.jenkins-ci.plugins.workflow:workflow-basic-steps"
  const val DEFAULT_WORKFLOW_DURABLE_TASK_STEP = "org.jenkins-ci.plugins.workflow:workflow-durable-task-step"
}
