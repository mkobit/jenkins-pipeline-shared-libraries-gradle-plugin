package com.mkobit

@Grab('org.apache.commons:commons-math3:3.4.1')
import org.apache.commons.math3.primes.Primes

class GrabUsingLibrary {
  private GrabUsingLibrary() {
  }

  static boolean isPrime(int number) {
    return Primes.isPrime(number)
  }
}
