require 'test/minirunit'
test_check "module"

# MRI 1.7-style self-replacement for define_method's blocks

class TestModule_Foo
  define_method(:foo) { self }
end
# MRI 1.6 returns Class, 1.7 returns Foo.
#test_equal(Class, TestModule_Foo.new.foo.class)
#test_equal(TestModule_Foo, TestModule_Foo.new.foo.class)

test_equal("TestModule_Foo", TestModule_Foo.new.foo.class.name)

testmodule_local_variable = 123

TestModule_Foo.module_eval {||
  def abc(x)
    2 * x
  end
  XYZ = 10
  ABC = self
  LOCAL1 = testmodule_local_variable
}
test_ok(! defined? abc)
test_equal(4, TestModule_Foo.new.abc(2))
test_equal(10, TestModule_Foo::XYZ)
test_equal(TestModule_Foo, TestModule_Foo::ABC)
test_equal(testmodule_local_variable, TestModule_Foo::LOCAL1)

class TestModule2
end
TestModule2.module_eval("def abc(x); 3 * x; end; XYZ = 12; ABC = self; LOCAL2 = testmodule_local_variable")
test_equal(6, TestModule2.new.abc(2))
test_equal(12, TestModule2::XYZ)
test_equal(TestModule2, TestModule2::ABC)
test_equal(testmodule_local_variable, TestModule2::LOCAL2)

module A
  module B
    module C
      test_equal([A::B::C, A::B, A], Module.nesting)
      $nest = Module.nesting
    end
    module D
      test_equal([A::B::D, A::B, A], Module.nesting)
    end
    test_equal([A::B, A], Module.nesting)
  end
end
test_equal([], Module.nesting)
test_equal([A::B::C, A::B, A], $nest)

OUTER_CONSTANT = 4711

module TestModule_A
  A_CONSTANT = 123
  class TestModule_B
    attr_reader :a
    attr_reader :b
    def initialize
      @a = A_CONSTANT
      @b = OUTER_CONSTANT
    end
  end
end
test_equal(123, TestModule_A::TestModule_B.new.a)
test_equal(4711, TestModule_A::TestModule_B.new.b)

class TestModule_C_1 < Array
  def a_defined_method
    :ok
  end
end
class TestModule_C_1
end
test_equal(:ok, TestModule_C_1.new.a_defined_method)


#test_exception(TypeError) {
#  module Object; end
#}
test_exception(TypeError) {
  class Kernel; end
}


################# test external reference to constant from included module
module M1
  CONST = 7
end
class C1
  include M1
  x = CONST
end

test_equal(7, C1::CONST)

################ test define_method

class C2
  define_method( 'methodName', proc { 1 })
  e = test_exception(TypeError) {
    define_method( 'methodNameX', 'badParameter')
  }
  test_equal('wrong argument type String (expected Proc/RubyMethod)', e.message)
end
class C3 < C2
  define_method( 'methodName2', instance_method(:methodName))
end