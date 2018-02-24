package com.mkobit

import com.cloudbees.groovy.cps.NonCPS

class LibHelper {
  private script
  LibHelper(script) {
    this.script = script
  }

  void sayHelloTo(String name) {
    script.echo("LibHelper says hello to ${'$'}name!")
  }

  @NonCPS
  List<Integer> increment(List<Integer> ints) {
    return ints.collect { it + 1 }
  }
}
