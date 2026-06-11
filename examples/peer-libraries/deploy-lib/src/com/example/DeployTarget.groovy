package com.example

class DeployTarget implements Serializable {
    String env
    String service

    String format() {
        return "${service} → ${env}"
    }
}
