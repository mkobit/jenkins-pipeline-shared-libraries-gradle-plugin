package com.mkobit.jenkins.pipelines

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.gradle.testfixtures.ProjectBuilder

internal class SharedLibraryExtensionTest : DescribeSpec({
  fun extension(
    pipelineUnit: String = "3.0",
    testHarness: String = "4.0",
  ): SharedLibraryExtension {
    val project = ProjectBuilder.builder().build()
    return SharedLibraryExtension(
      project.initializedProperty(pipelineUnit),
      project.initializedProperty(testHarness),
    )
  }

  describe("default versions") {
    val ext = extension()

    it("exposes the pipeline unit version") {
      ext.pipelineTestUnitVersion.get() shouldBe "3.0"
    }

    it("exposes the test harness version") {
      ext.testHarnessVersion.get() shouldBe "4.0"
    }
  }

  describe("mutable versions") {
    it("can override the pipeline unit version") {
      val ext = extension()
      ext.pipelineTestUnitVersion.set("newPipelineVersion")
      ext.pipelineTestUnitVersion.get() shouldBe "newPipelineVersion"
    }

    it("can override the test harness version") {
      val ext = extension()
      ext.testHarnessVersion.set("newTestHarnessVersion")
      ext.testHarnessVersion.get() shouldBe "newTestHarnessVersion"
    }
  }
})
