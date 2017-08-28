package com.mkobit

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.jvnet.hudson.test.JenkinsRule

class WorkflowJobUsageTest {

  @Rule
  public JenkinsRule rule = new JenkinsRule()

  @Before
  void configureRule() {
    rule.timeout = 30
  }

  @Test
  void canUseJenkins() {
    final WorkflowJob project = rule.createProject(WorkflowJob)
    project.definition = new CpsFlowDefinition('echo "Hello ${env.JOB_NAME}"')

    rule.buildAndAssertSuccess(project)
  }
}
