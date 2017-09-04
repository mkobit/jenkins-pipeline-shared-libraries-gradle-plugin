package testsupport

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag

@Disabled("test or feature not implemented yet")
annotation class NotImplementedYet

@Tag("possible-sample")
annotation class SampleCandidate

@Tag("integration")
annotation class Integration
