import hudson.model.Result;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Investigates whether @Library in a vars script bridges the per-library
 * GroovyClassLoader isolation in Jenkins.
 *
 * deploy-lib declares shell-lib as a peer, so com.example.ShellStep compiles
 * fine (Gradle wires peer src/ onto compileClasspath). crossImport.groovy
 * declares @Library('shell-lib') at the top of the vars script itself —
 * the hypothesis being that this loads shell-lib's classloader as a parent
 * of deploy-lib's classloader, making ShellStep resolvable at runtime.
 */
@WithJenkins
class CrossLibrarySrcImportTest {

    @Test
    void crossSrcImportFails_withoutLibraryAnnotation(JenkinsRule jenkins) throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class);
        // crossImport.groovy has @Library('shell-lib') in its own script body — removed here to
        // show the baseline: without any @Library, the import fails.
        job.setDefinition(new CpsFlowDefinition("echo crossImport()", false));
        jenkins.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0));
    }

    @Test
    void crossSrcImportFails_withLibraryAnnotationOnlyInPipelineScript(JenkinsRule jenkins) throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class);
        // @Library in the Jenkinsfile adds shell-lib's src/ to the pipeline script's classloader,
        // but not to deploy-lib's classloader where crossImport.groovy runs.
        job.setDefinition(new CpsFlowDefinition(
            "@Library('shell-lib') _\necho crossImport()", false));
        jenkins.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0));
    }

    @Test
    void crossSrcImportSucceeds_withLibraryAnnotationInVarsScript(JenkinsRule jenkins) throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class);
        // crossImport.groovy itself declares @Library('shell-lib') — if this causes Jenkins to
        // share shell-lib's classloader with deploy-lib's vars context, ShellStep is resolvable.
        job.setDefinition(new CpsFlowDefinition("echo crossImport()", false));
        WorkflowRun run = jenkins.buildAndAssertSuccess(job);
        jenkins.assertLogContains("shell: cross-import-test", run);
    }
}
