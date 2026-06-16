import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class RunDeployTest {

    @Test
    void deploysPipelineUsingAllLibrariesIncludingTransitive(JenkinsRule jenkins) throws Exception {
        var job = jenkins.createProject(WorkflowJob.class);
        // `notifier` is configured with implicit=false so a Jenkinsfile must opt in via @Library.
        // The other peers (deployer, pre-checks, shell-utils) remain implicit and auto-load.
        job.setDefinition(new CpsFlowDefinition("""
            @Library('notifier') _
            runDeploy('api-service', 'production')
            """, false));
        var run = jenkins.buildAndAssertSuccess(job);
        jenkins.assertLogContains("Pre-checks passed for api-service", run);
        jenkins.assertLogContains("shell: deploy api-service", run);
        jenkins.assertLogContains("Deploying api-service → production", run);
        jenkins.assertLogContains("[slack] api-service deployed to production", run);
    }
}
