require 'minirunit'
test_check "module"

# MRI 1.7-style self-replacement for define_method's blocks

class TestModule_Foo
  define_method(:foo) { self }
end
# MRI 1.6 returns Class, 1.7 returns Foo.
#test_equal(Class, TestModule_Foo.new.foo.type)
#test_equal(TestModule_Foo, TestModule_Foo.new.foo.type)

test_ok(TestModule_Foo.new.foo.kind_of?(Class))
