import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class SayHelloStepTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void defaultGreeting() throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("sayHello()", true));
        WorkflowRun run = jenkins.buildAndAssertSuccess(job);
        jenkins.assertLogContains("Hello, world!", run);
    }

    @Test
    public void namedGreeting() throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("sayHello('Jenkins')", true));
        WorkflowRun run = jenkins.buildAndAssertSuccess(job);
        jenkins.assertLogContains("Hello, Jenkins!", run);
    }
}
