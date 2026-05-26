import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertTrue

class SayHelloStepTest extends BasePipelineTest {

    @BeforeEach
    void setup() throws Exception {
        setUp()
    }

    @Test
    void defaultGreeting() throws Exception {
        def script = loadScript('vars/sayHello.groovy')
        script.invokeMethod('call', [] as Object[])
        assertTrue(helper.callStack.any { it.toString().contains('Hello, world!') })
    }

    @Test
    void namedGreeting() throws Exception {
        def script = loadScript('vars/sayHello.groovy')
        script.invokeMethod('call', ['Jenkins'] as Object[])
        assertTrue(helper.callStack.any { it.toString().contains('Hello, Jenkins!') })
    }
}
