import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertTrue

class RunDeployTest extends BasePipelineTest {

    @BeforeEach
    void setup() throws Exception {
        setUp()
        helper.registerAllowedMethod('preCheck', [String]) { service -> "Pre-checks passed for ${service}" }
        helper.registerAllowedMethod('runShell', [String]) { cmd -> "shell: ${cmd}" }
        helper.registerAllowedMethod('deployTo', [String, String]) { env, service -> "Deploying ${service} to ${env}" }
        helper.registerAllowedMethod('notifySlack', [String]) { msg -> "[slack] ${msg}" }
    }

    @Test
    void pipelineMocksPeerStepsAndSucceeds() {
        def script = loadScript('vars/runDeploy.groovy')
        script.invokeMethod('call', ['api-service', 'production'] as Object[])
        assertTrue(helper.callStack.any { it.toString().contains('preCheck(api-service)') })
        assertTrue(helper.callStack.any { it.toString().contains('runShell(deploy api-service)') })
        assertTrue(helper.callStack.any { it.toString().contains('deployTo(production, api-service)') })
        assertTrue(helper.callStack.any { it.toString().contains('notifySlack(api-service deployed to production)') })
    }
}
