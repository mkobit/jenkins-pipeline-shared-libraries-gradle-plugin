import com.example.Greeter

def call(String name = 'world') {
    def greeter = new Greeter()
    echo greeter.greet(name)
}
