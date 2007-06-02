require 'test/minirunit'

class Object
  def add(name, c)
    class_eval("#{name} = c")
  end
end

module A1
  add("FOO", String)
  def do
    FOO.new("foo")
  end
end

class B1
  include A1
end

test_equal(["FOO"], A1.constants)
test_equal(["FOO"], B1.constants)
test_equal(B1.const_get("FOO"), String)
test_equal("foo", B1.new.do)

# tests nesting of classes inside modues
module A2
  class B2
    add("FOO", String)
    def do
      FOO.new("foo")
    end
  end
end

test_equal(["FOO"], A2::B2.constants)
test_equal(A2::B2.const_get("FOO"), String)
test_equal("foo", A2::B2.new.do)

# confirm class_eval is nested the same as the calling scope
class A3
  class B3
    test_equal([A3::B3, A3], Module.nesting)
  end
end

test_equal([A3::B3], A3::B3.class_eval("Module.nesting"))

class C3
  test_equal([A3::B3, C3], A3::B3.class_eval("Module.nesting"))
end

# tests lookup of :: scoped classes
class A4
  class B4
  end
end

class A4::B4
  class ::A4
    test_ok(self == A4)
    def foo
      "foo"
    end
  end
  
  class ::C4
  end
end

test_equal("foo", A4.new.foo)
test_ok(C4)