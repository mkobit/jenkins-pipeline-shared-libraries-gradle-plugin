package com.example

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import testsupport.kotest.JenkinsFunSpec

class SayHelloStepTest : JenkinsFunSpec({
    test("default greeting") {
        jenkins { j ->
            val job = j.createProject(WorkflowJob::class.java)
            job.definition = CpsFlowDefinition("sayHello()", true)
            j.assertLogContains("Hello, world!", j.buildAndAssertSuccess(job))
        }
    }

    test("named greeting") {
        jenkins { j ->
            val job = j.createProject(WorkflowJob::class.java)
            job.definition = CpsFlowDefinition("sayHello('Jenkins')", true)
            j.assertLogContains("Hello, Jenkins!", j.buildAndAssertSuccess(job))
        }
    }
})
