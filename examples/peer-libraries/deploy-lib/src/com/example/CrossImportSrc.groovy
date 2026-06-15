package com.example

class CrossImportSrc implements Serializable {
    String run() {
        return new com.example.ShellStep().run("cross-src-import-test")
    }
}
