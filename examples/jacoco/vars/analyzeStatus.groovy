import com.example.BuildStatusAnalyzer

def call(String status) {
    def analyzer = new BuildStatusAnalyzer()
    echo analyzer.analyze(status)
}
