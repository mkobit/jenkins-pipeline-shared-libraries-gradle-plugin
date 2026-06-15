import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Proves cross-library src/ imports work natively in Jenkins.
 *
 * deploy-lib's crossImport.groovy plainly imports com.example.ShellStep (defined in shell-lib),
 * and deploy-lib's CrossImportSrc class plainly references it too. No @Library annotation,
 * no merged sources. This works because Jenkins gives every library in a pipeline run the same
 * CpsGroovyShell$CleanGroovyClassLoader — classes loaded for one library are visible to all
 * other libraries in that run. The earlier "isolation" failures were a plugin-side artifact
 * where direct peers' compiled classes were leaked onto the integration test JVM classpath
 * via implementation-extends-from-sharedLibraryDependencies; with that removed, all libraries
 * round-trip through the same Jenkins loader and cross-imports just work.
 */
@WithJenkins
class CrossLibrarySrcImportTest {

    @Test
    void varsScriptCanImportSrcClassFromPeerLibrary(JenkinsRule jenkins) throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("echo crossImport()", false));
        WorkflowRun run = jenkins.buildAndAssertSuccess(job);
        jenkins.assertLogContains("shell: cross-import-test", run);
    }

    @Test
    void srcClassCanReferenceSrcClassFromPeerLibrary(JenkinsRule jenkins) throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("echo crossImportSrc()", false));
        WorkflowRun run = jenkins.buildAndAssertSuccess(job);
        jenkins.assertLogContains("shell: cross-src-import-test", run);
    }
}
