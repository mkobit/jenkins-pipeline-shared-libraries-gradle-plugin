package com.example.shell

import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

class ShellStepTest {

    @Test
    void runPrefixesOutputWithShellLabel() {
        assertEquals('shell: ls -la', new ShellStep().run('ls -la').toString())
    }
}
