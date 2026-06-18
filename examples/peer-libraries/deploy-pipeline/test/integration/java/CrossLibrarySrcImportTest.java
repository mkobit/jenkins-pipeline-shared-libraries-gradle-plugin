import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * deploy-lib's steps and src class freely import com.example.shell.ShellStep from shell-lib
 * — peer libraries can reference each other's classes by their normal package paths.
 */
@WithJenkins
class CrossLibrarySrcImportTest {

    // sandbox=false: the bundled groovy-all on Jenkins 2.479.x LTS conflicts with the sandbox
    // resolver. See the "Groovy 3.x" note in the root README; once Jenkins 2.492+ is the floor
    // we can flip to true and add coverage for both.
    private static final boolean SANDBOX = false;

    @Test
    void varsScriptCanImportClassFromPeerLibrarySrc(JenkinsRule jenkins) throws Exception {
        var job = jenkins.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("echo restartService('api-service')", SANDBOX));
        var run = jenkins.buildAndAssertSuccess(job);
        jenkins.assertLogContains("shell: systemctl restart api-service", run);
    }

    @Test
    void srcClassCanImportClassFromPeerLibrarySrc(JenkinsRule jenkins) throws Exception {
        var job = jenkins.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("echo runHealthCheck('api-service')", SANDBOX));
        var run = jenkins.buildAndAssertSuccess(job);
        jenkins.assertLogContains("shell: healthcheck api-service", run);
    }
}
