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

c = Class.new
test_equal(Object, c.superclass)
c = Class.new(String)
test_equal(String, c.superclass)
test_exception(TypeError) {
  Class.new(Kernel)
}

module TestClasses
  testClass = Class.new

  TestClass = testClass
  test_equal('TestClass', testClass.name)

  DifferentNameForTestClass = testClass
  test_equal('TestClass', testClass.name)

  testModule = Module.new

  TestModule = testModule
  test_equal('TestModule', testModule.name)

  DifferentNameForTestModule = testModule
  test_equal('TestModule', testModule.name)
end

