import com.example.shell.ShellStep

def call(String service) {
    return new ShellStep().run("systemctl restart ${service}")
}
