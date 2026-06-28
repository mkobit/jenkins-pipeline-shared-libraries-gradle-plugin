import hudson.model.Result
import org.junit.Rule
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification

/**
 * Pinning test for issue #159 — Spock 2.x integration tests cannot use sandbox=true
 * on currently supported Jenkins LTS versions.
 *
 * Spock 2.x brings groovy:3.x onto the test classpath; Jenkins still bundles
 * groovy-all:2.4.21 in the WAR. With sandbox=true the CPS transformer runs inside
 * the test JVM and generates bytecode that the JVM rejects ("Cannot reference 'toArray'
 * before supertype constructor"). With sandbox=false the CPS transformer is bypassed
 * and tests run cleanly (see SayHelloStepSpec).
 *
 * This spec pins the current failure mode. The day Jenkins drops groovy-all from the
 * WAR — or Spock ships a groovy 2.x compatible variant — the build will start
 * succeeding, this spec will fail, and that is the signal to revisit #159.
 */
class SandboxLimitationPinSpec extends Specification {

    @Rule
    JenkinsRule jenkins = new JenkinsRule()

    def "sandbox=true currently fails compilation — remove this pin when it starts passing (#159)"() {
        given:
        def job = jenkins.createProject(WorkflowJob)
        job.setDefinition(new CpsFlowDefinition("sayHello()", true))

        when:
        def run = jenkins.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0))

        then:
        jenkins.assertLogContains("Cannot reference 'toArray' before supertype constructor", run)
    }
}
