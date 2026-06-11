package com.example

@Library('shell-lib')
class CrossImportSrc implements Serializable {
    String run() {
        return new com.example.ShellStep().run("cross-src-import-test")
    }
}
