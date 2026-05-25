plugins {
    id("com.mkobit.jenkins.pipelines.shared-library")
}

codenarc {
    configFile = file("config/codenarc/codenarc.xml")
}

sharedLibrary {
    pipelineUnitVersion = libs.versions.pipeline.unit
    jenkins {
        version = libs.versions.jenkins.core
        bomVersion = libs.versions.jenkins.bom
    }
    plugins {
        plugin(libs.lockable.resources)
        plugin(libs.milestone)
        plugin(libs.stage)
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
