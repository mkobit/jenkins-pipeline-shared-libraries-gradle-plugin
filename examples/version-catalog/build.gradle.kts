plugins {
    id("com.mkobit.jenkins.pipelines.shared-library")
}

sharedLibrary {
    pipelineUnitVersion = libs.versions.pipeline.unit
    jenkins {
        version = libs.versions.jenkins.core
        bomVersion = libs.versions.jenkins.bom
    }
    plugins {
        plugin(libs.jenkins.plugins.input)
        plugin(libs.jenkins.plugins.lock)
        plugin(libs.jenkins.plugins.milestone)
        plugin(libs.jenkins.plugins.stage)
    }
}

testing {
    suites {
        named<JvmTestSuite>("test") {
            dependencies {
                // TODO(#161): remove once the plugin auto-adds groovy alongside jenkins-pipeline-unit
                // https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin/issues/161
                implementation(libs.groovy)
            }
        }
    }
}
