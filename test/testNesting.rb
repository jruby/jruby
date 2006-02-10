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

