package testsupport

import com.mkobit.jenkins.pipelines.SharedLibraryDefaults

const val WORKFLOW_API = "org.jenkins-ci.plugins.workflow:workflow-api"

const val DEFAULT_CORE_VERSION = SharedLibraryDefaults.CORE_VERSION
const val DEFAULT_BOM_VERSION = SharedLibraryDefaults.BOM_VERSION

// Mirrors SharedLibraryDefaults.TEST_HARNESS_VERSION (internal); update both together.
const val DEFAULT_TEST_HARNESS_VERSION = "2565.vd1eb_7c961d1b_"
const val DEFAULT_PIPELINE_UNIT_VERSION = SharedLibraryDefaults.PIPELINE_UNIT_VERSION
