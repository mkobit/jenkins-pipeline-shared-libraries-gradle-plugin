def call() {
    def lines = []
    def cl = this.getClass().getClassLoader()
    while (cl != null) {
        lines << "metrics-loader: ${cl.getClass().getName()}@${System.identityHashCode(cl)}"
        cl = cl.getParent()
    }
    lines << "metrics-loader: <bootstrap>"
    return lines.join('\n')
}
