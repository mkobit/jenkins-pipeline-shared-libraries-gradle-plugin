import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Cross-library src/ imports work natively: a class defined in one peer's src/ can be referenced
 * by another peer's vars or src with a plain import, because all libraries in a pipeline run
 * share one CpsGroovyShell classloader.
 */
@WithJenkins
class CrossLibrarySrcImportTest {

    @Test
    void varsScriptCanImportSrcClassFromPeerLibrary(JenkinsRule jenkins) throws Exception {
        var job = jenkins.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("echo crossImport()", false));
        var run = jenkins.buildAndAssertSuccess(job);
        jenkins.assertLogContains("shell: cross-import-test", run);
    }

    @Test
    void srcClassCanReferenceSrcClassFromPeerLibrary(JenkinsRule jenkins) throws Exception {
        var job = jenkins.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("echo crossImportSrc()", false));
        var run = jenkins.buildAndAssertSuccess(job);
        jenkins.assertLogContains("shell: cross-src-import-test", run);
    }
}
