package com.mkobit

import com.mkobit.jenkins.pipelines.codegen.LocalLibraryRetriever
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.libs.GlobalLibraries
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration
import org.jenkinsci.plugins.workflow.libs.LibraryRetriever
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.jvnet.hudson.test.JenkinsRule

class GrabLibraryIntegrationTest {

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
    GlobalLibraries.get().libraries = [localLibrary]
  }

  @Test
  void testingMyLibrary() {
    CpsFlowDefinition flow = new CpsFlowDefinition('''
      import com.mkobit.GrabUsingLibrary
      
      echo "Result: ${GrabUsingLibrary.isPrime(5)}"
    '''.stripIndent(), true)
    WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'project')
    workflowJob.definition = flow
    rule.assertLogContains('Result: true', rule.buildAndAssertSuccess(workflowJob))
  }
}
