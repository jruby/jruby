require 'minirunit'
test_check "Test Classes"
class Hello
    def saveHelloWorld
        @hello = "Hello World."
    end
    def getHelloWorld
        @hello
    end
end

hello = Hello.new
test_ok("Hello World." == hello.saveHelloWorld)
test_ok("Hello World." == hello.getHelloWorld)

