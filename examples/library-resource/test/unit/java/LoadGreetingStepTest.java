import com.lesfurets.jenkins.unit.BasePipelineTest;
import groovy.lang.Script;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoadGreetingStepTest extends BasePipelineTest {

    @BeforeEach
    void setup() throws Exception {
        setUp();
    }

    @Test
    void loadsAndEchoesGreeting() throws Exception {
        Script script = loadScript("vars/loadGreeting.groovy");
        script.invokeMethod("call", new Object[]{});
        assertTrue(getHelper().getCallStack().stream().anyMatch(c -> c.toString().contains("Hello from a library resource!")));
    }
}
