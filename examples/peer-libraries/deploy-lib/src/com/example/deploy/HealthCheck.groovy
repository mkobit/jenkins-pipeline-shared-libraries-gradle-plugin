package com.example.deploy

import com.example.shell.ShellStep

class HealthCheck implements Serializable {
    String run(String service) {
        return new ShellStep().run("healthcheck ${service}")
    }
}
