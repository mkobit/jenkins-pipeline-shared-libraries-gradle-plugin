def call(String environment) {
    milestone()
    input id: 'approve', message: "Deploy to ${environment}?", ok: "Deploy"
    lock(resource: "env-${environment}") {
        stage("Deploy ${environment}") {
            echo "Deploying to ${environment}"
        }
    }
}
