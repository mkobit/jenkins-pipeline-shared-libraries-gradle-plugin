import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

class RunShellTest extends BasePipelineTest {

    @BeforeEach
    void setup() throws Exception {
        setUp()
    }

    @Test
    void prefixesCommandWithShellLabel() {
        def script = loadScript('vars/runShell.groovy')
        assertEquals('shell: ls -la', script.call('ls -la').toString())
    }
}
