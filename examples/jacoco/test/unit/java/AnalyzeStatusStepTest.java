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

    private void invokeAndAssertLogged(Object status, String expected) throws Exception {
        Script script = loadScript("vars/analyzeStatus.groovy");
        script.invokeMethod("call", new Object[]{status});
        assertTrue(
            getHelper().getCallStack().stream().anyMatch(c -> c.toString().contains(expected)),
            "expected call stack to contain: " + expected
        );
    }

    @Test
    void successStatus() throws Exception {
        invokeAndAssertLogged("SUCCESS", "The build completed successfully. Great job!");
    }

    @Test
    void failureStatus() throws Exception {
        invokeAndAssertLogged("FAILURE", "The build failed. Please check the logs for errors.");
    }

    @Test
    void unstableStatus() throws Exception {
        invokeAndAssertLogged("UNSTABLE", "The build is unstable. Some tests might be failing.");
    }

    @Test
    void abortedStatus() throws Exception {
        invokeAndAssertLogged("ABORTED", "The build was aborted manually.");
    }

    @Test
    void unrecognizedStatus() throws Exception {
        invokeAndAssertLogged("WAT", "Unrecognized build status: WAT.");
    }

    @Test
    void unknownStatus() throws Exception {
        invokeAndAssertLogged(null, "Status is unknown.");
    }

    @Test
    void notBuiltStatusBypassesAnalyzer() throws Exception {
        invokeAndAssertLogged("NOT_BUILT", "Pipeline has not been built yet.");
    }
}
