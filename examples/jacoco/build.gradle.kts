plugins {
    id("com.mkobit.jenkins.pipelines.shared-library")
    jacoco
}

tasks.jacocoTestReport {
    // JenkinsPipeline shared library dynamically generates classes, leading to class mismatch warnings
    // or missing execution data, so we filter those out.
    classDirectories.setFrom(
        classDirectories.files.map {
            fileTree(it).matching {
                // Filter out the global vars as they are interpreted
                exclude("sayHello*")
            }
        }
    )
}

// Ensure the JaCoCo report is generated after tests run
tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.integrationTest {
    // Testing jenkins-test-harness not in scope for this example so skip Integration tests
    enabled = false
}
