package com.mkobit

import org.junit.Before
import org.junit.Test
import com.lesfurets.jenkins.unit.BasePipelineTest

class JenkinsPipelineUnitUsageTest extends BasePipelineTest {

  @Override
  @Before
  void setUp() throws Exception {
    super.setUp()
  }

  @Test
  void canExecutePipelineJob() {
    runScript("example.jenkins")
    printCallStack()
    assertJobStatusSuccess()
  }
}
