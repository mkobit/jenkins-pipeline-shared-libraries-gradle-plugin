import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class PublishReleaseTest {

    @Test
    void publishReleaseStampsVersionAndLogsUrl(JenkinsRule jenkins) throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition(
            "publishRelease('catalog-api', '2.1.0', '99', 'prod')", false));
        WorkflowRun run = jenkins.buildAndAssertSuccess(job);
        jenkins.assertLogContains("Released catalog-api@2.1.0+build.99 to https://catalog-api.prod.internal", run);
        jenkins.assertLogContains("[SUCCESS] catalog-api", run);
    }
}
