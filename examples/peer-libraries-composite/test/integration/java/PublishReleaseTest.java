import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class PublishReleaseTest {

    // See peer-libraries/CrossLibrarySrcImportTest for the sandbox=false rationale.
    private static final boolean SANDBOX = false;

    @Test
    void publishReleaseStampsVersionAndLogsUrl(JenkinsRule jenkins) throws Exception {
        var job = jenkins.createProject(WorkflowJob.class);
        // pipeline-notifier is configured with implicit=false; pipelines that emit a build
        // report must opt in via @Library. pipeline-deployer (and its transitive
        // version-utils) remain implicit and auto-load.
        job.setDefinition(new CpsFlowDefinition("""
            @Library('pipeline-notifier') _
            publishRelease('catalog-api', '2.1.0', '99', 'prod')
            """, SANDBOX));
        var run = jenkins.buildAndAssertSuccess(job);
        jenkins.assertLogContains("Released catalog-api@2.1.0+build.99 to https://catalog-api.prod.internal", run);
        jenkins.assertLogContains("[SUCCESS] catalog-api", run);
    }
}
