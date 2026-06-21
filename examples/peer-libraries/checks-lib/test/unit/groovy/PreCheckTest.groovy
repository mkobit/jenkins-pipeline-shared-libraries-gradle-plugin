import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

class PreCheckTest extends BasePipelineTest {

    @BeforeEach
    void setup() throws Exception {
        setUp()
    }

    @Test
    void reportsPassedChecksForService() {
        def script = loadScript('vars/preCheck.groovy')
        assertEquals('Pre-checks passed for api-service', script.call('api-service').toString())
    }
}
