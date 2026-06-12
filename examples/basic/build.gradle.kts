plugins {
    id("com.mkobit.jenkins.pipelines.shared-library")
}

sharedLibrary {
    plugins {
        plugin("org.jenkins-ci.plugins.workflow:workflow-basic-steps")
        plugin("org.jenkins-ci.plugins.workflow:workflow-durable-task-step")
    }
}
