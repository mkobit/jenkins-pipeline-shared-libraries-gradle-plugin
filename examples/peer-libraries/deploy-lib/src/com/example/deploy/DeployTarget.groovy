package com.example.deploy

class DeployTarget implements Serializable {
    String env
    String service

    String format() {
        return "${service} → ${env}"
    }
}
