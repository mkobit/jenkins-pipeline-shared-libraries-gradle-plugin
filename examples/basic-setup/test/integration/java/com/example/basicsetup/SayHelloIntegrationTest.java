package com.example.basicsetup;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class SayHelloIntegrationTest {

  @Test
  void sayHelloStepLogsExpectedOutput(JenkinsRule rule) throws Exception {
    var job = rule.createProject(WorkflowJob.class, "say-hello");
    job.setDefinition(new CpsFlowDefinition("sayHello('World')", true));
    WorkflowRun run = rule.buildAndAssertSuccess(job);
    rule.assertLogContains("Hello, World!", run);
  }
}
