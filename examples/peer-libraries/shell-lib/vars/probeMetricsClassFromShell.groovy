// Runs in shell-lib's classloader. Tries to load com.example.MetricsRecorder
// (defined in metrics-lib, a sibling transitive peer). If shell-lib and metrics-lib
// truly share a classloader, this should succeed without any @Library hint.
def call() {
    try {
        def cls = this.getClass().getClassLoader().loadClass("com.example.MetricsRecorder")
        return "probe: SUCCESS — loaded ${cls.getName()} via ${this.getClass().getClassLoader().getClass().getName()}"
    } catch (ClassNotFoundException e) {
        return "probe: FAILURE — ClassNotFoundException for com.example.MetricsRecorder"
    }
}
