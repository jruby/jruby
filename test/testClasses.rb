require 'test/minirunit'
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
test_equal("Hello World." , hello.saveHelloWorld)
test_equal("Hello World." , hello.getHelloWorld)

