import com.lesfurets.jenkins.unit.BasePipelineTest;
import groovy.lang.Script;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AnalyzeStatusStepTest extends BasePipelineTest {

    @BeforeEach
    void setup() throws Exception {
        setUp();
    }

    @Test
    void successStatus() throws Exception {
        Script script = loadScript("vars/analyzeStatus.groovy");
        script.invokeMethod("call", new Object[]{"SUCCESS"});
        assertTrue(getHelper().getCallStack().stream().anyMatch(c -> c.toString().contains("The build completed successfully. Great job!")));
    }

    @Test
    void unknownStatus() throws Exception {
        Script script = loadScript("vars/analyzeStatus.groovy");
        script.invokeMethod("call", new Object[]{null});
        assertTrue(getHelper().getCallStack().stream().anyMatch(c -> c.toString().contains("Status is unknown.")));
    }
}
