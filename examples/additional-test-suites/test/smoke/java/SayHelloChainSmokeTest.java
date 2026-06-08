import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class SayHelloChainSmokeTest {

    @Test
    void chainedGreetingsAllSucceed(JenkinsRule jenkins) throws Exception {
        var job = jenkins.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition(
            """
            sayHello()
            sayHello('Alice')
            sayHello('Bob')
            """,
            true
        ));
        WorkflowRun run = jenkins.buildAndAssertSuccess(job);
        jenkins.assertLogContains("Hello, world!", run);
        jenkins.assertLogContains("Hello, Alice!", run);
        jenkins.assertLogContains("Hello, Bob!", run);
    }
}
