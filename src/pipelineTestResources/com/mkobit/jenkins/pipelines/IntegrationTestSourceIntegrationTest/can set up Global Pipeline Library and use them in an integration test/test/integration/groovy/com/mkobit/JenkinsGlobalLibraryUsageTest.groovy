package com.mkobit

import jenkins.plugins.git.GitSCMSource
import org.jenkinsci.plugins.workflow.libs.GlobalLibraries
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration
import org.jenkinsci.plugins.workflow.libs.SCMSourceRetriever
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.jvnet.hudson.test.JenkinsRule

// tag::test-class[]
class JenkinsGlobalLibraryUsageTest {

  @Rule
  public JenkinsRule rule = new JenkinsRule()

  @Before
  void configureGlobalGitLibraries() {
    rule.timeout = 30
    final libraryPath = System.getProperty('user.dir')
    println("Using Git library path at $libraryPath")
    final SCMSourceRetriever retriever = new SCMSourceRetriever(
      new GitSCMSource(
        null,
        libraryPath,
        '',
        'local-source-code',
        // Fetch everything - if this is not used builds fail on Jenkins for some reason
        '*:refs/remotes/origin/*',
        '*',
        '',
        true
      )
    )
    final LibraryConfiguration localLibrary =
      new LibraryConfiguration('pipelineUtilities', retriever)
    localLibrary.implicit = true
    localLibrary.defaultVersion = 'git rev-parse HEAD'.execute().text.trim()
    localLibrary.allowVersionOverride = false
    GlobalLibraries.get().setLibraries(Collections.singletonList(localLibrary))
  }

  @Test
  void testingMyLibrary() {
    final CpsFlowDefinition flow = new CpsFlowDefinition('''
import com.mkobit.LibHelper

final libHelper = new LibHelper(this)
libHelper.sayHelloTo('mkobit')
    ''', true)
    final WorkflowJob workflowJob = rule.createProject(WorkflowJob, 'project')
    workflowJob.definition = flow
    rule.buildAndAssertSuccess(workflowJob)
  }
}
// end:test-class[]
