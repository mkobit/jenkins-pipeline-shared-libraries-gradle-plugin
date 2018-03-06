package com.mkobit

import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper

class LibraryUsingJenkinsClasses {

  private final CpsScript script

  LibraryUsingJenkinsClasses(CpsScript script) {
    this.script = Objects.requireNonNull(script)
  }

  void showJobCount() {
    script.echo "Job count: ${Jenkins.instance.jobNames.size()}"
  }

  void showCpsFlowDefinition() {
    RunWrapper wrapper = script.currentBuild
    if (wrapper.rawBuild instanceof WorkflowRun) {
      WorkflowRun run = wrapper.rawBuild as WorkflowRun
      WorkflowJob job = run.parent
      if (job.definition instanceof CpsFlowDefinition) {
        CpsFlowDefinition definition = job.definition as CpsFlowDefinition
        script.echo "Script of job definition: ${definition.script}"
      } else {
        script.echo "Job definition is not an instance of CpsFlowDefinition"
      }
    } else {
      throw new RuntimeException()
    }
  }
}
