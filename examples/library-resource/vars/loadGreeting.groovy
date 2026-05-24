def call() {
    def greeting = libraryResource('com/example/greeting.txt')
    echo greeting
}
