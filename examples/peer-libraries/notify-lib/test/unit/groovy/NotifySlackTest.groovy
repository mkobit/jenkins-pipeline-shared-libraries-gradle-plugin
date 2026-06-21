import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

class NotifySlackTest extends BasePipelineTest {

    @BeforeEach
    void setup() throws Exception {
        setUp()
    }

    @Test
    void prefixesMessageWithSlackChannel() {
        def script = loadScript('vars/notifySlack.groovy')
        assertEquals('[slack] deployment complete', script.call('deployment complete').toString())
    }
}
