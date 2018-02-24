package com.mkobit

import com.mkobit.jenkins.pipelines.codegen.LocalLibraryRetriever
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.jenkinsci.plugins.workflow.libs.GlobalLibraries
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration
import org.jenkinsci.plugins.workflow.libs.LibraryRetriever
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.jvnet.hudson.test.JenkinsRule

class JenkinsGlobalLibraryUsageTest {

  @Rule
  public JenkinsRule rule = new JenkinsRule()

  @Before
  void configureGlobalLibraries() {
    rule.timeout = 30
    final LibraryRetriever retriever = new LocalLibraryRetriever()
    final LibraryConfiguration localLibrary =
      new LibraryConfiguration('testLibrary', retriever)
    localLibrary.implicit = true
    localLibrary.defaultVersion = 'unused'
    localLibrary.allowVersionOverride = false
    GlobalLibraries.get().setLibraries(Collections.singletonList(localLibrary))
  }

  @Test
  void testingMyLibrary() {
    CpsFlowDefinition flow = new CpsFlowDefinition('''
      import com.mkobit.LibHelper
      
      final libHelper = new LibHelper(this)
      libHelper.sayHelloTo('mkobit')
    '''.stripIndent(), true)
    WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'project')
    workflowJob.definition = flow
    rule.buildAndAssertSuccess(workflowJob)
  }

  @Test
  void testingNonCpsMethod() {
    CpsFlowDefinition flow = new CpsFlowDefinition('''
      import com.mkobit.LibHelper
      
      final libHelper = new LibHelper(this)
      echo "Numbers: ${libHelper.increment([1,2])}"
    '''.stripIndent(), true)
    WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'project')
    workflowJob.definition = flow
    WorkflowRun workflowRun = rule.buildAndAssertSuccess(workflowJob)
    rule.assertLogContains('Numbers: [2, 3]', workflowRun)
  }
}
