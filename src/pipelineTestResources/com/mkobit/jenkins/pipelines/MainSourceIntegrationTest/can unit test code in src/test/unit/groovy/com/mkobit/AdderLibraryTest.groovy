package com.mkobit

import org.junit.Assert
import org.junit.Test

class MyLibTest {

  @Test
  void checkAddition() {
    def library = new AdderLibrary()
    Assert.assertEquals(3, library.add(1, 2))
  }
}
