import com.example.deploy.DeployTarget

def call(String env, String service) {
    def target = new DeployTarget(env: env, service: service)
    return "Deploying ${target.format()}"
}
