package com.example.deploy

import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

class DeployTargetTest {

    @Test
    void formatIncludesServiceAndEnv() {
        def target = new DeployTarget(env: 'production', service: 'api-service')
        assertEquals('api-service → production', target.format().toString())
    }
}
