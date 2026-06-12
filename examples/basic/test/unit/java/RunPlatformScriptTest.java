import com.lesfurets.jenkins.unit.BasePipelineTest;
import groovy.lang.Script;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RunPlatformScriptTest extends BasePipelineTest {

    @BeforeEach
    void setup() throws Exception {
        setUp();
    }

    @Test
    void testRunPlatformScript() throws Exception {
        Script script = loadScript("vars/runPlatformScript.groovy");
        script.invokeMethod("call", new Object[]{});
        // Verify that either the Unix or Windows branch executed successfully
        assertTrue(getHelper().getCallStack().stream().anyMatch(c -> c.toString().contains("Executing on Unix") || c.toString().contains("Executing on Windows")));
    }
}
