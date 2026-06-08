import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Verifies that shared library Groovy classes are usable directly in pipeline scripts,
 * not only through vars — and that instances survive CPS step boundaries (Serializable).
 */
@WithJenkins
class CpsClassUsageTest {

    @Test
    void sharedLibraryClassSurvivesCpsStepBoundary(JenkinsRule jenkins) throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class);
        // Greeter is captured in a local variable and used across multiple CPS steps.
        // Each `echo` is a CPS suspension point; the Greeter instance must be serializable.
        job.setDefinition(new CpsFlowDefinition(
            "def g = new com.example.Greeter()\n"
            + "echo g.greet('First')\n"
            + "echo g.greet('Second')",
            true
        ));
        WorkflowRun run = jenkins.buildAndAssertSuccess(job);
        jenkins.assertLogContains("Hello, First!", run);
        jenkins.assertLogContains("Hello, Second!", run);
    }
}
