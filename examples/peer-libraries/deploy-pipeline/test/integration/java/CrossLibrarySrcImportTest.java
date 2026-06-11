import hudson.model.Result;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Investigates whether @Library can bridge Jenkins' per-library GroovyClassLoader isolation.
 *
 * deploy-lib declares shell-lib as a peer, so com.example.ShellStep compiles fine in Gradle
 * (peer src/ is on compileClasspath). At runtime Jenkins gives each library its own isolated
 * GroovyClassLoader. The disabled tests below assert the desired behavior — that @Library in
 * various positions causes Jenkins to make ShellStep visible to deploy-lib's scripts.
 * Currently all fail with ClassNotFoundException; @Disabled tracks them as known open questions.
 */
@WithJenkins
class CrossLibrarySrcImportTest {

    @Test
    void crossSrcImportFails_withoutLibraryAnnotation(JenkinsRule jenkins) throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class);
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

    @Disabled("hypothesis: @Library in a vars script should cause Jenkins to load shell-lib's " +
              "classloader before compiling the script, making ShellStep visible")
    @Test
    void crossSrcImportSucceeds_withLibraryAnnotationInVarsScript(JenkinsRule jenkins) throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("echo crossImport()", false));
        WorkflowRun run = jenkins.buildAndAssertSuccess(job);
        jenkins.assertLogContains("shell: cross-src-import-test", run);
    }

    @Disabled("hypothesis: @Library on a src/ class should trigger classloader bridging " +
              "before Jenkins compiles the class, making ShellStep visible")
    @Test
    void crossSrcImportSucceeds_withLibraryAnnotationOnSrcClass(JenkinsRule jenkins) throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("echo crossImportSrc()", false));
        WorkflowRun run = jenkins.buildAndAssertSuccess(job);
        jenkins.assertLogContains("shell: cross-src-import-test", run);
    }
}
