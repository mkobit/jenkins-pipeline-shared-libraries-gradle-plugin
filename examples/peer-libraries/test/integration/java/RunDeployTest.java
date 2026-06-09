import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class RunDeployTest {

    @Test
    void deploysPipelineUsingAllLibrariesIncludingTransitive(JenkinsRule jenkins) throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("runDeploy('api-service', 'production')", false));
        WorkflowRun run = jenkins.buildAndAssertSuccess(job);
        jenkins.assertLogContains("Pre-checks passed for api-service", run);
        jenkins.assertLogContains("shell: deploy api-service", run);
        jenkins.assertLogContains("Deploying api-service to production", run);
        jenkins.assertLogContains("[slack] api-service deployed to production", run);
    }
}
