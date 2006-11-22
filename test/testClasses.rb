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
  test_equal('TestClasses::TestClass', testClass.name)

  DifferentNameForTestClass = testClass
  test_equal('TestClasses::TestClass', testClass.name)

  testModule = Module.new

  TestModule = testModule
  test_equal('TestClasses::TestModule', testModule.name)

  DifferentNameForTestModule = testModule
  test_equal('TestClasses::TestModule', testModule.name)

  def TestClasses.virtual
    class << self
      self
    end
  end
end

begin
  class X < Foo::Bar
  end
  fail
rescue NameError => e
  test_equal("uninitialized constant Foo", e.to_s)
end

begin
  class X < TestClasses::Bar
  end
  fail
rescue NameError => e
  test_equal("uninitialized constant TestClasses::Bar", e.to_s)
end

begin
  class X < Class
  end
  fail
rescue TypeError => e
  test_equal("can't make subclass of Class", e.to_s)
end

begin 
  class X < TestClasses.virtual
  end
rescue TypeError => e
  test_equal("can't make subclass of virtual class", e.to_s)
end

class MockObject
  def self.mock methodName
    define_method "showBug" do 
      @results ||= {}
      @results["C"] = "Z"
      fail "Hash should have something" if @results == {}
      @results ||= {}
      fail "||= destroyed a perfectly good hash" if @results == {}
    end
  end
  mock :foo
end

mock = MockObject.new
mock.showBug 
