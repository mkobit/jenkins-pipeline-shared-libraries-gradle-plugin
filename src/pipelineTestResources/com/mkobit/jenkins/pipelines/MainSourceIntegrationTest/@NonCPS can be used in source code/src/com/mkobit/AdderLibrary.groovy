package com.mkobit

import com.cloudbees.groovy.cps.NonCPS

class SubtracterLibrary {
  @NonCPS
  int subtract(int a, int b) {
    return a + b
  }
}
