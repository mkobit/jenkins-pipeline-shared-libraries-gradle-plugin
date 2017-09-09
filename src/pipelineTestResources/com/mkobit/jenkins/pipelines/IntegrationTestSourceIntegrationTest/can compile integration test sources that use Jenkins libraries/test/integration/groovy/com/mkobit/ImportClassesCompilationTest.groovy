package com.mkobit

import org.jenkinsci.plugins.workflow.libs.GlobalLibraries
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration
import org.jenkinsci.plugins.workflow.libs.SCMSourceRetriever
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.junit.Assert
import org.junit.Test

class ImportClassesCompilationTest {

  @Test
  void canAccessClass() {
    Assert.assertNotNull(GlobalLibraries.class)
    Assert.assertNotNull(LibraryConfiguration.class)
    Assert.assertNotNull(SCMSourceRetriever.class)
    Assert.assertNotNull(CpsFlowDefinition.class)
    Assert.assertNotNull(WorkflowJob.class)
  }
}
