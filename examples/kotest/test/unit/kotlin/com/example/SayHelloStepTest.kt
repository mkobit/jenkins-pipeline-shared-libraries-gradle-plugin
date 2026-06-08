package com.example

import com.lesfurets.jenkins.unit.BasePipelineTest
import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.forAtLeastOne
import io.kotest.matchers.string.shouldContain
import org.codehaus.groovy.runtime.InvokerHelper

class SayHelloStepTest : FunSpec({
    lateinit var base: BasePipelineTest

    beforeEach {
        base =
            object : BasePipelineTest() {}.also { t ->
                t.setUp()
            }
    }

    test("default greeting") {
        InvokerHelper.invokeMethod(base.loadScript("vars/sayHello.groovy"), "call", null)
        base.helper.callStack.forAtLeastOne { it.toString() shouldContain "Hello, world!" }
    }

    test("named greeting") {
        InvokerHelper.invokeMethod(base.loadScript("vars/sayHello.groovy"), "call", "Jenkins")
        base.helper.callStack.forAtLeastOne { it.toString() shouldContain "Hello, Jenkins!" }
    }
})
