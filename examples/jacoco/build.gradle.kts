plugins {
    id("com.mkobit.jenkins.pipelines.shared-library")
    jacoco
}

tasks.jacocoTestReport {
    // Vars compile to the default package and are recompiled at runtime by JenkinsPipelineUnit,
    // causing class ID mismatches. Excluding default-package classes drops vars from the report.
    classDirectories.setFrom(
        classDirectories.files.map {
            fileTree(it).matching {
                exclude("*.class")
            }
        }
    )
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}
