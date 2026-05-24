import com.lesfurets.jenkins.unit.BasePipelineTest;
import groovy.lang.Script;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SayHelloStepTest extends BasePipelineTest {

    @BeforeEach
    void setup() throws Exception {
        setScriptRoots(new String[]{"."});
        setUp();
    }

    @Test
    void defaultGreeting() throws Exception {
        Script script = loadScript("vars/sayHello.groovy");
        script.invokeMethod("call", new Object[]{});
        assertTrue(getHelper().getCallStack().stream().anyMatch(c -> c.toString().contains("Hello, world!")));
    }

    @Test
    void namedGreeting() throws Exception {
        Script script = loadScript("vars/sayHello.groovy");
        script.invokeMethod("call", new Object[]{"Jenkins"});
        assertTrue(getHelper().getCallStack().stream().anyMatch(c -> c.toString().contains("Hello, Jenkins!")));
    }
}
