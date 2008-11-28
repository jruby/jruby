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
b, a = 1, 1
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

$! = nil
defined? NoSuchThing::ToTestSideEffect
test_equal(nil, $!)

# Constants in toplevel should be searched last
Gobble = "hello"
module Foo2
  Gobble = "goodbye"
end
class Bar2
  include Foo2
  def gobble
    Gobble
  end
end
test_equal("goodbye", Bar2.new.gobble)

# Test fix for JRUBY-1339 ("Constants nested in a Module are not included")

module Outer
  class Inner
  end
end

def const_from_name(name)
  const = ::Object
  name.sub(/\A::/, '').split('::').each do |const_str|
    if const.const_defined?(const_str)
      const = const.const_get(const_str)
      next
    end
    return nil
  end
  const
end

include Outer

test_equal(Outer::Inner, const_from_name("Inner"))
test_equal("constant", defined?Inner)

# End test fix for JRUBY-1339

# JRUBY-2004
test_exception(TypeError) {
  JRuby2004 = 5
  JRuby2004::X = 5
}

# JRUBY-3091
JRuby3091CONST1 = 1
class JRuby3091A1; end
class JRuby3091B1 < JRuby3091A1
  def const; JRuby3091CONST1; end
end

test_equal(1, JRuby3091B1.new.const)
JRuby3091A1.const_set(:JRuby3091CONST1, 2)
test_equal(2, JRuby3091B1.new.const)

JRuby3091CONST2 = 1
class JRuby3091A2
  class JRuby3091B2
    def const; JRuby3091CONST2; end
  end
end

test_equal(1, JRuby3091A2::JRuby3091B2.new.const)
JRuby3091A2.const_set(:JRuby3091CONST2, 2)
test_equal(2, JRuby3091A2::JRuby3091B2.new.const)

# More JRUBY-3091
# 7867 broke const lookup by not eventually searching Object *and* its supers
module JRuby3091A3
  JRuby3091CONST3 = 1
end

class Object
  include JRuby3091A3
end

module JRuby3091B3
  def self.const
    JRuby3091CONST3
  end
end

test_equal(1, JRuby3091B3.const)

module JRuby3117
  class ::Object
    def jruby3117
      JRuby3117Const
    end
  end
  JRuby3117Const = 1
end

test_no_exception {
  test_equal(1, jruby3117)
}