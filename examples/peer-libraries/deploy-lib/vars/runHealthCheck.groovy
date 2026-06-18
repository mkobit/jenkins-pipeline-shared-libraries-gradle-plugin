import com.example.deploy.HealthCheck

def call(String service) {
    return new HealthCheck().run(service)
}
