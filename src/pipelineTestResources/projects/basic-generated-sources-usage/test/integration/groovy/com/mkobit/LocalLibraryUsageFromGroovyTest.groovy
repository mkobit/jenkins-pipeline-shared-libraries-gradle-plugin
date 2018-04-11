package com.mkobit

import com.mkobit.jenkins.pipelines.codegen.LocalLibraryRetriever
import org.jenkinsci.plugins.workflow.libs.LibraryRetriever
import org.junit.Test

class LocalLibraryUsageFromGroovyTest {

  @Test
  void createRetriever() {
    final LibraryRetriever retriever = new LocalLibraryRetriever()
  }
}
