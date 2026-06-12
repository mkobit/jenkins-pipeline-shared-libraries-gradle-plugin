def call() {
    node {
        if (isUnix()) {
            echo "Executing on Unix"
        } else {
            echo "Executing on Windows"
        }
    }
}
