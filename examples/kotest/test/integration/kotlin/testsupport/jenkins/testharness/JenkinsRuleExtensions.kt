package testsupport.jenkins.testharness

import hudson.model.TopLevelItem
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jvnet.hudson.test.JenkinsRule

/**
 * Creates a project of the reified type [T], optionally with a [name].
 */
inline fun <reified T : TopLevelItem> JenkinsRule.createProject(name: String? = null): T =
    if (name != null) {
        createProject(T::class.java, name)
    } else {
        createProject(T::class.java)
    }

/**
 * Creates a [WorkflowJob] with the given [name], optional [definition] string, and [sandbox] setting.
 */
fun JenkinsRule.createWorkflowJob(name: String? = null, definition: String? = null, sandbox: Boolean = true): WorkflowJob {
    val job = createProject<WorkflowJob>(name)
    if (definition != null) {
        job.definition = CpsFlowDefinition(definition, sandbox)
    }
    return job
}
