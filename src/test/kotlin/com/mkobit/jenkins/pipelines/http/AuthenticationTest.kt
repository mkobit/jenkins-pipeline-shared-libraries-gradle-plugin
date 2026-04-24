package com.mkobit.jenkins.pipelines.http

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContain
import java.util.Base64

internal class AuthenticationTest :
  DescribeSpec({
    val username = "mkobit"
    val password = "this is the password"
    val expectedHeader = "Basic ${Base64.getEncoder().encodeToString("$username:$password".toByteArray())}"

    describe("BasicAuthentication") {
      it("produces a Basic Authorization header") {
        BasicAuthentication(username, password)
          .headers()
          .shouldContain("Authorization", expectedHeader)
      }
    }

    describe("ApiTokenAuthentication") {
      it("produces a Basic Authorization header using the token as password") {
        ApiTokenAuthentication(username, password)
          .headers()
          .shouldContain("Authorization", expectedHeader)
      }
    }

    describe("AnonymousAuthentication") {
      it("produces no headers") {
        AnonymousAuthentication.headers().shouldBeEmpty()
      }
    }
  })
