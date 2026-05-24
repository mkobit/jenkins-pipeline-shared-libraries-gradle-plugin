plugins {
    id("com.mkobit.jenkins.pipelines.shared-library")
}

codenarc {
    configFile = file("config/codenarc/codenarc.xml")
}

sharedLibrary {
    pipelineUnitVersion.set(libs.versions.pipelineUnit)
    jenkins {
        version.set(libs.versions.jenkins)
        bomVersion.set(libs.versions.jenkinsBom)
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
        named<JvmTestSuite>("integrationTest") {
        }
    }
}
