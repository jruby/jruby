require 'test/minirunit'
test_check "module"

# MRI 1.7-style self-replacement for define_method's blocks

class TestModule_Foo
  define_method(:foo) { self }
end
# MRI 1.6 returns Class, 1.7 returns Foo.
#test_equal(Class, TestModule_Foo.new.foo.type)
#test_equal(TestModule_Foo, TestModule_Foo.new.foo.type)

test_equal("TestModule_Foo", TestModule_Foo.new.foo.type.name)

TestModule_Foo.module_eval {||
  def abc(x)
    2 * x
  end
  XYZ = 10
  ABC = self
}
test_equal(4, TestModule_Foo.new.abc(2))
test_equal(10, TestModule_Foo::XYZ)
test_equal(TestModule_Foo, TestModule_Foo::ABC)

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
