import org.junit.Rule
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification

class SayHelloStepSpec extends Specification {

    @Rule
    JenkinsRule jenkins = new JenkinsRule()

    def "default greeting runs successfully with sandbox=false"() {
        given:
        def job = jenkins.createProject(WorkflowJob)
        job.setDefinition(new CpsFlowDefinition("sayHello()", false))

        when:
        def run = jenkins.buildAndAssertSuccess(job)

        then:
        jenkins.assertLogContains("Hello, world!", run)
    }

    def "named greeting runs successfully with sandbox=false"() {
        given:
        def job = jenkins.createProject(WorkflowJob)
        job.setDefinition(new CpsFlowDefinition("sayHello('Jenkins')", false))

        when:
        def run = jenkins.buildAndAssertSuccess(job)

        then:
        jenkins.assertLogContains("Hello, Jenkins!", run)
    }
}
