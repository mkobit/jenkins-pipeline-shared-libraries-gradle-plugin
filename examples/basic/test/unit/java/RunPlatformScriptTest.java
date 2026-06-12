import com.lesfurets.jenkins.unit.BasePipelineTest;
import groovy.lang.Script;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import groovy.lang.Closure;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RunPlatformScriptTest extends BasePipelineTest {

    @BeforeEach
    void setup() throws Exception {
        setUp();
    }

    @Test
    void testRunPlatformScriptUnix() throws Exception {
        getHelper().registerAllowedMethod("isUnix", Collections.emptyList(), new Closure<Boolean>(null, null) {
            public Boolean doCall(Object... args) {
                return true;
            }
        });
        Script script = loadScript("vars/runPlatformScript.groovy");
        script.invokeMethod("call", new Object[]{});

        assertTrue(getHelper().getCallStack().stream().anyMatch(c -> c.toString().contains("Executing on Unix")));
    }

    @Test
    void testRunPlatformScriptWindows() throws Exception {
        getHelper().registerAllowedMethod("isUnix", Collections.emptyList(), new Closure<Boolean>(null, null) {
            public Boolean doCall(Object... args) {
                return false;
            }
        });
        Script script = loadScript("vars/runPlatformScript.groovy");
        script.invokeMethod("call", new Object[]{});

        assertTrue(getHelper().getCallStack().stream().anyMatch(c -> c.toString().contains("Executing on Windows")));
    }
}
