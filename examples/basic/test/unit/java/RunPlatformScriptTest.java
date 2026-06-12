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
        // Just load the script and call it. JenkinsPipelineUnit doesn't execute sh or bat by default but intercepts it.
        Script script = loadScript("vars/runPlatformScript.groovy");
        script.invokeMethod("call", new Object[]{});
        // By default JenkinsPipelineUnit might consider isUnix() as false unless mocked, or we can just verify it doesn't crash.
    }
}
