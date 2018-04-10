package com.mkobit;

import com.mkobit.jenkins.pipelines.codegen.LocalLibraryRetriever;
import org.jenkinsci.plugins.workflow.libs.LibraryRetriever;
import org.junit.Test;

import java.util.Collections;

public class LocalLibraryUsageFromJavaTest {

  @Test
  public void createRetriever() {
    final LibraryRetriever retriever = new LocalLibraryRetriever();
  }
}
