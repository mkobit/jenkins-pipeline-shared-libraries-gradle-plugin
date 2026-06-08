import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class SayHelloStepTest {

    @Test
    void callsPeerLibraryStep(JenkinsRule jenkins) throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("sayHello('Jenkins')", false));
        WorkflowRun run = jenkins.buildAndAssertSuccess(job);
        jenkins.assertLogContains("Hello, Jenkins!", run);
    }

    @Test
    void defaultGreeting(JenkinsRule jenkins) throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("sayHello()", false));
        WorkflowRun run = jenkins.buildAndAssertSuccess(job);
        jenkins.assertLogContains("Hello, world!", run);
    }
}
