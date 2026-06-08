package com.example

class BuildStatusAnalyzer implements Serializable {
    private static final long serialVersionUID = 1L

    String analyze(String status) {
        if (status == null) {
            return "Status is unknown."
        }

        switch (status.toUpperCase()) {
            case 'SUCCESS':
                return "The build completed successfully. Great job!"
            case 'FAILURE':
                return "The build failed. Please check the logs for errors."
            case 'UNSTABLE':
                return "The build is unstable. Some tests might be failing."
            case 'ABORTED':
                return "The build was aborted manually."
            default:
                return "Unrecognized build status: ${status}."
        }
    }
}
