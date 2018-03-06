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

class LibraryUsingJenkinsClassesIntegrationTest {

  @Rule
  public JenkinsRule rule = new JenkinsRule()

  @Before
  void 'configure global library'() {
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
  void 'can use Jenkins core classes'() {
    CpsFlowDefinition flow = new CpsFlowDefinition('''
      import com.mkobit.LibraryUsingJenkinsClasses
      
      final library = new LibraryUsingJenkinsClasses(this)
      library.showJobCount()
    '''.stripIndent(), true)
    WorkflowJob workflowJob = rule.createProject(WorkflowJob)
    workflowJob.definition = flow
    rule.assertLogContains('Job count: 1', rule.buildAndAssertSuccess(workflowJob))
    rule.createProject(WorkflowJob)
    rule.assertLogContains('Job count: 2', rule.buildAndAssertSuccess(workflowJob))
  }

  @Test
  void 'can use plugin classes'() {
    CpsFlowDefinition flow = new CpsFlowDefinition('''
      import com.mkobit.LibraryUsingJenkinsClasses
      
      final library = new LibraryUsingJenkinsClasses(this)
      library.showCpsFlowDefinition()
    '''.stripIndent(), true)
    WorkflowJob workflowJob = rule.createProject(WorkflowJob)
    workflowJob.definition = flow
    WorkflowRun run = rule.buildAndAssertSuccess(workflowJob)
    rule.assertLogContains("Script of job definition: ${flow.script}", run)
  }
}
