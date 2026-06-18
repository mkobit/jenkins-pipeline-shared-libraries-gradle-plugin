package com.example.shell

class ShellStep implements Serializable {
    String run(String cmd) {
        return "shell: ${cmd}"
    }
}
