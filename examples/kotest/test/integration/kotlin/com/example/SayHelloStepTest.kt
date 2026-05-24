package com.example

import io.kotest.matchers.string.shouldContain
import testsupport.jenkins.testharness.*
import testsupport.kotest.JenkinsFunSpec

class SayHelloStepTest : JenkinsFunSpec({
    test("default greeting") {
        jenkins { j ->
            val job = j.createWorkflowJob(definition = "sayHello()")
            val run = j.buildAndAssertSuccess(job)
            run.log shouldContain "Hello, world!"
        }
    }

    test("named greeting") {
        jenkins { j ->
            val job = j.createWorkflowJob(definition = "sayHello('Jenkins')")
            val run = j.buildAndAssertSuccess(job)
            run.log shouldContain "Hello, Jenkins!"
        }
    }
})
