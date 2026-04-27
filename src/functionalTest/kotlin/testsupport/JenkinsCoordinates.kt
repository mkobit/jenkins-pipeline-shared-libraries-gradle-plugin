package testsupport

// Coordinates and defaults used across functional test build scripts.
// Keep in sync with SharedLibraryPlugin companion constants (same Jenkins LTS line).
const val JENKINS_BOM = "io.jenkins.tools.bom:bom-2.479.x:5054.v620b_5d2b_d5e6"
const val WORKFLOW_API = "org.jenkins-ci.plugins.workflow:workflow-api"

// Mirror of SharedLibraryPlugin defaults — update here when the plugin defaults change.
const val DEFAULT_CORE_VERSION = "2.479.1"
const val DEFAULT_TEST_HARNESS_VERSION = "2391.v9b_3e2d3351a_2"
