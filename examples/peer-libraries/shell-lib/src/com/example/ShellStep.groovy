package com.example

class ShellStep implements Serializable {
    String run(String cmd) {
        return "shell: ${cmd}"
    }
}
