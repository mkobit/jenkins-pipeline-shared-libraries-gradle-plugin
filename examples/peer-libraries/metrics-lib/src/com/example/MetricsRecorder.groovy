package com.example

class MetricsRecorder implements Serializable {
    String record(String name) {
        return "metric: ${name}"
    }
}
