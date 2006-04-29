require 'test/minirunit'
test_check "Test Constant Scoping:"

C1 = 1

module A
  C2 = 2

  module B
    class C
      C3 = 3

      def foo
        "ABC"
      end

      i1 = ""
      
      class << i1
        test_equal(3, C3)
      end
      
      i2 = Class.new
      
      class << i2
        test_equal(3, C3)
      end
    end
  end

  class << self
    test_equal(1, C1)
    test_equal(2, C2)
  end
end

class D < A::B::C
  test_equal(3, C3)
end

module A
  module B
    test_equal(1, C1)
    test_equal(2, C2)
  end
end

module B
  class C
    def foo
      "BC"
    end
  end
end

test_equal("ABC", A::B::C.new.foo)

module Foo
  class ObjectSpace
  end

  test_equal('ObjectSpace', ::ObjectSpace.name)
end

class Gamma
end

module Bar
        class Gamma
        end

	test_equal('Gamma', ::Gamma.name)
end

FOO = "foo"

test_equal(::FOO, "foo")

module X
   def X.const_missing(name)
     "missing"
   end
 end

test_equal(X::String, "missing")

module Y
  String = 1
end

test_equal(Y::String, 1)

module Z1
  ZOOM = "zoom"
  module Z2
    module Z3
      test_equal(ZOOM, "zoom")
      test_equal(Z1::ZOOM, "zoom")
    end
  end
end

# Should not cause collision
module Out
  Argument = "foo"
end

class Switch
  include Out
  class Argument < self
  end
end

# Should cause TypeError
# TODO: Figure out why test_exception(TypeError) {} is not working for this...
hack_pass = 0
begin
  class AAAA
    FOO = "foo"
    class FOO < self
    end
  end
rescue TypeError
  hack_pass = 1
end

test_ok(1, hack_pass)

# Should not cause collision
class Out2
  NoArgument = "foo"

  class Switch
    class NoArgument < self
    end
  end
end

module OutA
end

class OutA::InA
  def ok; "ok"; end
end

module OutA::InB
  OK = "ok"
end

test_ok("ok", OutA::InA.new.ok)
test_ok("ok", OutA::InB::OK)

test_ok("constant", defined? OutA)
test_equal(nil, defined? OutNonsense)

class Empty
end

# Declare constant outside of class/module
test_equal(1, Empty::FOOT = 1)
# Declare constant outside of class/module in multi assign
Empty::BART, a = 1, 1
test_equal(1, Empty::BART)
# Declare a constant whose value changes scope
CONST_SCOPE_CHANGE = begin
     require 'this_will_never_load'
     true
   rescue LoadError
     false
   end
test_equal(false, CONST_SCOPE_CHANGE)
Empty::CONST_FOO = begin
     require 'this_will_never_load'
     true
   rescue LoadError
     false
   end
test_equal(false, Empty::CONST_FOO)


