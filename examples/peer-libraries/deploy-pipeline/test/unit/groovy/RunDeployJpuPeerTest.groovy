import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static com.lesfurets.jenkins.unit.global.lib.LibraryConfiguration.library
import static com.lesfurets.jenkins.unit.global.lib.ProjectSource.projectSource
import static org.junit.jupiter.api.Assertions.assertTrue

/**
 * Probe: JenkinsPipelineUnit loading peer libraries for real (vs. mocking them with
 * registerAllowedMethod, as RunDeployTest does). Each peer's syncSharedLibrarySource task
 * stages sources at {peerProject}/build/sharedLibrarySource/{peerProject.name}/. JPU is
 * pointed at those directories via projectSource.
 *
 * Current friction this exposes:
 *   - Hard-coded relative paths into other subprojects' build dirs (brittle to refactors,
 *     and would break if libraryName overrides were used).
 *   - Each peer must be registered manually here; the plugin already knows the list and
 *     could surface it (the integrationTest suite gets it via test.library.N.* JVM args
 *     when useTestHarness=true, but JPU's `test` suite does not).
 *   - No analogue of SharedLibraryAutoRegistrar for JPU.
 */
class RunDeployJpuPeerTest extends BasePipelineTest {

    @BeforeEach
    void setup() throws Exception {
        setUp()
        registerPeerLibrary('deploy-lib', 'deployer')
        registerPeerLibrary('checks-lib', 'pre-checks')
        registerPeerLibrary('shell-lib', 'shell-utils')
        // notify-lib is composite (includeBuild) so its synced dir lives elsewhere; skip for now.
        helper.registerAllowedMethod('notifySlack', [String]) { msg -> "[slack] ${msg}" }
    }

    private void registerPeerLibrary(String projectName, String jenkinsLibraryName) {
        String location = "../${projectName}/build/sharedLibrarySource/${projectName}"
        helper.registerSharedLibrary(library()
            .name(jenkinsLibraryName)
            .retriever(projectSource(location))
            .targetPath(location)
            .defaultVersion('main')
            .allowOverride(true)
            .implicit(true)
            .build())
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
