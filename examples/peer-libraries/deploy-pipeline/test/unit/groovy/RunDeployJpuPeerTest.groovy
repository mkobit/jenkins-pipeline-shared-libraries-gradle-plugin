import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static com.lesfurets.jenkins.unit.global.lib.LibraryConfiguration.library
import static com.lesfurets.jenkins.unit.global.lib.ProjectSource.projectSource
import static org.junit.jupiter.api.Assertions.assertTrue

/**
 * JenkinsPipelineUnit loading peer libraries for real (vs. mocking them with
 * registerAllowedMethod, as RunDeployTest does). The plugin injects
 * test.library.N.{name,location,implicit} system properties on every test task,
 * so JPU tests can read them and register peer libraries without hard-coding
 * paths into sibling subprojects' build dirs.
 */
class RunDeployJpuPeerTest extends BasePipelineTest {

    @BeforeEach
    void setup() throws Exception {
        setUp()
        // Index 0 is the consumer's own library, indices 1+ are peers.
        int i = 1
        while (true) {
            String name = System.getProperty("test.library.${i}.name")
            String location = System.getProperty("test.library.${i}.location")
            if (name == null || location == null) break
            boolean implicit = !'false'.equalsIgnoreCase(
                System.getProperty("test.library.${i}.implicit", 'true'))
            helper.registerSharedLibrary(library()
                .name(name)
                .retriever(projectSource(location))
                .targetPath(location)
                .defaultVersion('main')
                .allowOverride(true)
                .implicit(implicit)
                .build())
            i++
        }
        // notify-lib is resolved via includeBuild; its library JAR lands on test compile classpath
        // but its synced source dir isn't currently surfaced by the plugin to JPU tests (only the
        // peer src dirs of project-style peers are). Mock notifySlack until that gap is closed.
        helper.registerAllowedMethod('notifySlack', [String]) { msg -> "[slack] ${msg}" }
    }

    @Test
    void loadsPeerVarsForReal() {
        def script = loadScript('vars/runDeploy.groovy')
        script.invokeMethod('call', ['api-service', 'production'] as Object[])
        // The strings in the call stack come from the consumer's runDeploy, which echoes
        // each peer step's return value. If JPU loaded the peer vars for real, the echoed
        // strings match what the actual peer scripts return (preCheck returns "Pre-checks
        // passed for X", runShell returns "shell: X", etc.). If JPU fell back to mocks,
        // the return values would be nulls and these substrings wouldn't appear.
        def transcript = helper.callStack*.toString().join('\n')
        assertTrue(transcript.contains('Pre-checks passed for api-service'),
            "preCheck output missing — peer wasn't really loaded\n" + transcript)
        assertTrue(transcript.contains('Deploying api-service'),
            "deployTo output missing — peer wasn't really loaded\n" + transcript)
    }
}
