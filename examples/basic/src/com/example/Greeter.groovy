package com.example

class Greeter implements Serializable {
    private static final long serialVersionUID = 1L

    String greet(String name) {
        "Hello, ${name}!"
    }
}
