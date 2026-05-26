import hudson.model.Result;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.support.steps.input.InputAction;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;

@WithJenkins
class DeployStepTest {

    @Test
    void approvedInputDeploysThenReleasesLock(JenkinsRule jenkins) throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("deploy('staging')", true));

        WorkflowRun run = job.scheduleBuild2(0).waitForStart();
        jenkins.waitForMessage("Deploy to staging?", run);
        run.getAction(InputAction.class).getExecution("Approve").proceed(null);

        jenkins.waitForCompletion(run);
        jenkins.assertBuildStatusSuccess(run);
        jenkins.assertLogContains("Deploying to staging", run);
    }

    @Test
    void stoppingBuildAtInputAborts(JenkinsRule jenkins) throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("deploy('staging')", true));

        WorkflowRun run = job.scheduleBuild2(0).waitForStart();
        jenkins.waitForMessage("Deploy to staging?", run);
        run.doStop();

        jenkins.waitForCompletion(run);
        assertEquals(Result.ABORTED, run.getResult());
    }

    @Test
    void newerBuildPassingMilestoneCancelsOlder(JenkinsRule jenkins) throws Exception {
        jenkins.jenkins.setNumExecutors(2);
        WorkflowJob job = jenkins.createProject(WorkflowJob.class);
        job.setConcurrentBuild(true);
        // An input gate before deploy() lets us release build2 first so it hits milestone() before build1
        job.setDefinition(new CpsFlowDefinition("""
                input id: 'proceed', message: 'Ready?'
                deploy('staging')
                """, true));

        WorkflowRun build1 = job.scheduleBuild2(0).waitForStart();
        jenkins.waitForMessage("Ready?", build1);

        WorkflowRun build2 = job.scheduleBuild2(0).waitForStart();
        jenkins.waitForMessage("Ready?", build2);

        // Release build2 (newer) first — it hits milestone() which marks build1 NOT_BUILT
        build2.getAction(InputAction.class).getExecution("Proceed").proceed(null);

        jenkins.waitForCompletion(build1);
        assertEquals(Result.NOT_BUILT, build1.getResult());

        // build2 is now at deploy's input; approve it and let it finish
        jenkins.waitForMessage("Deploy to staging?", build2);
        build2.getAction(InputAction.class).getExecution("Approve").proceed(null);

        jenkins.waitForCompletion(build2);
        jenkins.assertBuildStatusSuccess(build2);
        jenkins.assertLogContains("Deploying to staging", build2);
    }
}
