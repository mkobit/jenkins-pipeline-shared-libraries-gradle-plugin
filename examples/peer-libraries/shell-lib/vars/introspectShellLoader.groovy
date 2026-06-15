// Walks the classloader chain from this step's defining class up through parents
// and emits one line per loader so a Jenkins test can read the actual hierarchy.
def call() {
    def lines = []
    def cl = this.getClass().getClassLoader()
    while (cl != null) {
        lines << "shell-loader: ${cl.getClass().getName()}@${System.identityHashCode(cl)}"
        cl = cl.getParent()
    }
    lines << "shell-loader: <bootstrap>"
    return lines.join('\n')
}
