def call(String service, String env) {
    echo preCheck(service)
    echo runShell("deploy ${service}")
    echo deployTo(env, service)
    echo notifySlack("${service} deployed to ${env}")
}
