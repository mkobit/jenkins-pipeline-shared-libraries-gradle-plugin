import com.lesfurets.jenkins.unit.BasePipelineTest;
import groovy.lang.Closure;
import groovy.lang.Script;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DeployStepTest extends BasePipelineTest {

    @BeforeEach
    void setup() throws Exception {
        setScriptRoots(new String[]{"."});
        setUp();
        getHelper().registerAllowedMethod("milestone", new ArrayList<>(), (Closure<?>) null);
        getHelper().registerAllowedMethod("input", Arrays.asList(LinkedHashMap.class), (Closure<?>) null);
        getHelper().registerAllowedMethod("lock", Arrays.asList(LinkedHashMap.class, Closure.class), (Closure<?>) null);
    }

    @Test
    void stepsAreInvokedForEnvironment() throws Exception {
        Script script = loadScript("vars/deploy.groovy");
        script.invokeMethod("call", new Object[]{"production"});
        assertTrue(getHelper().getCallStack().stream().anyMatch(c -> c.toString().contains("milestone")));
        assertTrue(getHelper().getCallStack().stream().anyMatch(c -> c.toString().contains("approve")));
    }
}
