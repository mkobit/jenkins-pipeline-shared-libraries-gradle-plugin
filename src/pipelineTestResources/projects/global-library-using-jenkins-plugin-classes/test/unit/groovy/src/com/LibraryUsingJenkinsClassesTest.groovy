package com.mkobit

import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

class LibraryUsingJenkinsClassesTest {

  @Rule
  public ExpectedException exception = ExpectedException.none()

  @Test
  void 'throws exception for null constructor'() {
    exception.expect(NullPointerException)
    new LibraryUsingJenkinsClasses(null)
  }
}
