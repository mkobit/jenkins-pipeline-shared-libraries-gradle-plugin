def call() {
    def lines = []
    def cl = this.getClass().getClassLoader()
    while (cl != null) {
        lines << "deploy-loader: ${cl.getClass().getName()}@${System.identityHashCode(cl)}"
        cl = cl.getParent()
    }
    lines << "deploy-loader: <bootstrap>"
    return lines.join('\n')
}
