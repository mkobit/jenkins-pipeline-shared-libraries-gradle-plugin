def call(String env, String service) {
    return "https://${service}.${env}.internal"
}
