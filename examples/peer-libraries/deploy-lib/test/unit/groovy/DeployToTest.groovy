import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

class DeployToTest extends BasePipelineTest {

    @BeforeEach
    void setup() throws Exception {
        setUp()
    }

    @Test
    void formatsDeploymentMessage() {
        def script = loadScript('vars/deployTo.groovy')
        assertEquals('Deploying api-service to production', script.call('production', 'api-service').toString())
    }
}
