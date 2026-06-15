import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Asserts that every library loaded into a pipeline run shares one CpsGroovyShell
 * classloader. Regression guard: if the plugin ever re-leaks peer compiled classes
 * onto the integration test JVM classpath (or Jenkins changes its model), direct-peer
 * scripts would load via AppClassLoader and stop sharing a loader with transitive peers,
 * which is what previously hid the actual Jenkins behavior from our tests.
 *
 * Each introspect*Loader vars step prints `<lib>-loader: <ClassName>@<identityHashCode>`.
 * If all three start with the same CpsGroovyShell$CleanGroovyClassLoader identity, the
 * shared-classloader invariant holds and cross-library src/ imports work natively.
 */
@WithJenkins
class ClassloaderIntrospectionTest {

    @Test
    void allThreeLibrariesShareOneCpsGroovyShellClassloader(JenkinsRule jenkins) throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("""
            echo introspectDeployLoader()
            echo introspectShellLoader()
            echo introspectMetricsLoader()
            echo probeMetricsClassFromShell()
            """, false));
        WorkflowRun run = jenkins.buildAndAssertSuccess(job);

        String log = run.getLog();
        String deployLoader = leafLoaderIdentity(log, "deploy-loader");
        String shellLoader = leafLoaderIdentity(log, "shell-loader");
        String metricsLoader = leafLoaderIdentity(log, "metrics-loader");

        if (!deployLoader.contains("CpsGroovyShell$CleanGroovyClassLoader")) {
            throw new AssertionError("deploy-lib not loaded by CpsGroovyShell — leak regression?\n" + log);
        }
        if (!deployLoader.equals(shellLoader) || !deployLoader.equals(metricsLoader)) {
            throw new AssertionError("""
                expected all three libraries to share one CleanGroovyClassLoader
                  deploy:  %s
                  shell:   %s
                  metrics: %s
                """.formatted(deployLoader, shellLoader, metricsLoader));
        }
        jenkins.assertLogContains("probe: SUCCESS", run);
    }

    // Each introspect var prints "<lib>-loader: <ClassName>@<identityHash>" for the script's
    // own classloader before walking parents. Strip the "<lib>-loader: " prefix so the leaf
    // identities can be compared across libraries.
    private static String leafLoaderIdentity(String log, String prefix) {
        String marker = prefix + ":";
        for (String line : log.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(marker)) {
                return trimmed.substring(marker.length()).trim();
            }
        }
        throw new AssertionError("no '" + marker + "' line in log:\n" + log);
    }
}
