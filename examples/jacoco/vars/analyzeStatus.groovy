import com.example.BuildStatusAnalyzer

def call(String status) {
    if (status == "NOT_BUILT") {
        echo "Pipeline has not been built yet."
        return
    }
    def analyzer = new BuildStatusAnalyzer()
    echo analyzer.analyze(status)
}
