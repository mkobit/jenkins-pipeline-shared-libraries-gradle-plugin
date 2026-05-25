def call(String environment) {
    milestone()
    stage("Deploy ${environment}") {
        lock(resource: "env-${environment}") {
            echo "Deploying to ${environment}"
        }
    }
}
