package com.mkobit.jenkins.pipelines

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

internal class JenkinsPluginRuleTest :
  DescribeSpec({
    describe("isJenkinsPluginGroup") {
      withData(
        nameFn = { "returns true for $it" },
        "org.jenkins-ci.plugins",
        "org.jenkins-ci.plugins.workflow",
        "org.jenkins-ci.modules",
        "io.jenkins.plugins",
        "io.jenkins.plugins.something",
        "org.jenkinsci.plugins",
        "io.jenkins",
        "io.jenkins.blueocean",
        "org.6wind.jenkins",
        "com.cloudbees.jenkins.plugins",
        "com.cloudbees.plugins",
      ) { group ->
        JenkinsPluginRule.isJenkinsPluginGroup(group).shouldBeTrue()
      }

      withData(
        nameFn = { "returns false for $it" },
        "org.jenkins-ci.main",
        "com.example",
        "org.apache.commons",
        "io.spring",
        "org.jenkins",
        "",
      ) { group ->
        JenkinsPluginRule.isJenkinsPluginGroup(group).shouldBeFalse()
      }
    }
  })
