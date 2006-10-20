require 'test/minirunit'
test_check "Test Class derivation:"

class Top
  def Top.inherited(sub)
     $hierarchy << sub
  end
end

$hierarchy = [Top]
class Middle < Top
end

class Bottom < Middle
end

test_equal([Top, Middle, Bottom] , $hierarchy)


class AttrTest
  attr :attribute1
  attr_writer :attribute1
end
attrTest = AttrTest.new
attrTest.attribute1 = 1
test_equal(1 , attrTest.attribute1)


class Froboz
  include Enumerable
end
f = Froboz.new
test_equal([Froboz, Enumerable, Object, Kernel], f.class.ancestors)
test_ok(f.kind_of?(Froboz))
test_ok(f.kind_of?(Enumerable))

class CM1
end

class CM2
  def CM2::const_missing (constant)
    constant.to_s
  end
end

test_exception(NameError) {CM1::A}
test_equal(CM2::A, "A")

class GV1
  def initialize
    @@a = 1;
  end
  def modifyAfterFreeze
  	freeze
  	@aa = 2;
  end
  def createAfterFreeze
  	@@b = 2;
  end 
end

g = GV1.new
test_exception(TypeError) { g.modifyAfterFreeze }
test_no_exception {g = GV1.new}
g.class.freeze
test_exception(TypeError) {g.createAfterFreeze}

module A
  class Failure
    def Failure.bar()
      print "bar\n"
    end
  end
end
 
module B
  class Failure
    def Failure.foo()
      print "foo\n"
    end
  end
end
 
test_exception(NameError) {B::Failure.bar}

E = Hash.new
class << E
  def a() "A" end
end
test_equal("A", E.a)
test_exception(NoMethodError) { Hash.new.a }
test_exception(NoMethodError) { Object.new.a }

# test singleton method scoping
class C
  VAR1 = 1
  
  def C.get_var1
    VAR1
  end
  
  class << self
    VAR2 = 2
    
    def other_get_var1
      VAR1
    end
    
    def get_var2
      VAR2
    end
  end
end

test_equal(1, C.get_var1)
test_equal(1, C.other_get_var1)
test_equal(2, C.get_var2)

# ensure scoping of above methods does not change with new singleton class decl
class << C
end

test_equal(1, C.get_var1)
test_equal(1, C.other_get_var1)
test_equal(2, C.get_var2)

a = Class.new do
 def method_missing(name, *args) 
   self.class.send(:define_method, name) do |*a|
     "#{name}"
   end
   send(name)
 end
end

b = a.new

test_equal("foo", b.foo)

# test class var declaration
class ClassVarTest
  @@foo = "foo"
  test_equal(@@foo, "foo")
  
  def self.x
    @@bar = "bar"
    @@foo = "foonew"
    
    test_equal(@@bar, "bar")
    test_equal(@@foo, "foonew")
  end
  
  # call after self.x
  def z
    @@baz = "baz"
    
    test_equal(@@foo, "foonew")
    test_equal(@@bar, "bar")
    test_equal(@@baz, "baz")
  end
  
  def y
    @@foo
  end
end

test_no_exception {ClassVarTest.x }
test_no_exception {ClassVarTest.new.z }
test_equal(ClassVarTest.new.y, "foonew")

class TestClassVarAssignmentInSingleton
  @@a = nil

  class << self
    def bar
      test_equal(nil, @@a)
      @@a = 1 unless @@a
      test_equal(1, @@a)
    end
  end
end

TestClassVarAssignmentInSingleton.bar

# test define_method behavior to be working properly
$foo_calls = []
class BaseClass
def foo
$foo_calls << BaseClass
end
end

class SubClass < BaseClass
define_method(:foo) do
$foo_calls << SubClass
super
end
end

x = SubClass.new
test_no_exception { x.foo }
test_equal([SubClass, BaseClass], $foo_calls)

class NoConstantInInstanceVariables
  @@b = 4
  B = 2
end

test_equal(["@@b"], NoConstantInInstanceVariables.new.class.instance_variables)
