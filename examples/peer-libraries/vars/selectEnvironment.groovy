def call(String branch) {
    echo "Deploying ${branch} to ${targetEnv(branch)}"
}
