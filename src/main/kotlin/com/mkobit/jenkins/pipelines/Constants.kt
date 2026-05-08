package com.mkobit.jenkins.pipelines

// ── Test suite names / source set names ──────────────────────────────────────

internal const val INTEGRATION_TEST_SUITE = "integrationTest"
internal const val LOCAL_LIBRARY_RETRIEVER_SOURCE_SET = "localLibraryRetriever"

// ── Gradle configuration names ────────────────────────────────────────────────

internal const val JENKINS_PLUGIN_CONFIGURATION = "jenkinsPlugin"
internal const val JENKINS_PLUGIN_HPIS_CONFIGURATION = "jenkinsPluginHpis"
internal const val JENKINS_WAR_CONFIGURATION = "jenkinsWar"
internal const val GROOVY_ALL_RUNTIME_CONFIGURATION = "integrationTestGroovyAllRuntime"
internal const val IVY_CONFIGURATION = "sharedLibraryIvy"

// ── Dependency coordinates ────────────────────────────────────────────────────

internal const val GROOVY_ALL_MODULE = "org.codehaus.groovy:groovy-all"
internal const val IVY_COORDINATES = "org.apache.ivy:ivy:2.4.0"
internal const val ANNOTATION_INDEXER = "org.jenkins-ci:annotation-indexer:1.17"

// ── Default Jenkins plugin modules (version-free; managed by the BOM) ─────────

internal const val PIPELINE_GROOVY_LIB_MODULE = "io.jenkins.plugins:pipeline-groovy-lib"
internal const val WORKFLOW_JOB_MODULE = "org.jenkins-ci.plugins.workflow:workflow-job"
internal const val WORKFLOW_BASIC_STEPS_MODULE = "org.jenkins-ci.plugins.workflow:workflow-basic-steps"
internal const val WORKFLOW_DURABLE_TASK_STEP_MODULE = "org.jenkins-ci.plugins.workflow:workflow-durable-task-step"
