import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class RunPlatformScriptTest {

    @Test
    void testRunPlatformScript(JenkinsRule jenkins) throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("runPlatformScript()", true));
        WorkflowRun run = jenkins.buildAndAssertSuccess(job);
        if (java.io.File.separatorChar == '/') {
            jenkins.assertLogContains("Executing on Unix", run);
        } else {
            jenkins.assertLogContains("Executing on Windows", run);
        }
    }
}
