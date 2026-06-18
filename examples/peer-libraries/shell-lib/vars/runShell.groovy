import com.example.shell.ShellStep

def call(String cmd) {
    return new ShellStep().run(cmd)
}
