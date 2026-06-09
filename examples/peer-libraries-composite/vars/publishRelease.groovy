def call(String service, String version, String buildNum, String env) {
    def label = semverLabel(version, buildNum)
    def url = deploymentUrl(env, service)
    def report = buildReport(service, 'SUCCESS')
    echo "Released ${service}@${label} to ${url}"
    echo report
}
