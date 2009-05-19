require 'test/minirunit'
test_check "Test Object"


class Dummy
  attr_reader :var
  
  def initialize
    @var = 99
  end
  
  def remove
    remove_instance_variable(:@var)
  end
  
  def remove_bad
    remove_instance_variable(:@foo)
  end
end

d = Dummy.new
test_equal(99, d.var)
test_equal(99, d.remove)
test_equal(nil, d.var)
test_exception(NameError) { d.remove_bad }



#test singleton_methods
module Other
  def three() end
end

class Single
  def Single.four() end
end

a = Single.new

def a.one() end

class << a
  include Other
  def two() end
end

test_equal(%w(four),Single.singleton_methods)

a1 = a.singleton_methods(false)
a2 = a.singleton_methods(true)
a3 = a.singleton_methods

test_equal(2,a1.length)
test_ok(a1.include?('one'))
test_ok(a1.include?('two'))

test_equal(3,a2.length)
test_ok(a2.include?('one'))
test_ok(a2.include?('two'))
test_ok(a2.include?('three'))

test_equal(3,a3.length)
test_ok(a3.include?('one'))
test_ok(a3.include?('two'))
test_ok(a3.include?('three'))

class TestClz
  def call_private_bad; self.private_method; end
  def call_private_good; private_method; end
  private
  def private_method; 1; end
end
  
class PrivateSetter
  def foo
    self.setter=:bar
  end

  private
  def setter= arg
  end
end

test_exception(NoMethodError) { TestClz.new.private_method }
test_exception(NoMethodError) { TestClz.new.call_private_bad }
test_equal 1, TestClz.new.call_private_good
test_no_exception { PrivateSetter.new.foo }

# JRUBY-147: These all crashed at one point.  
# FIXME: These could be merged into more meaningful tests.
test_no_exception do
  o1 = Object.new
  class << o1; end
  o2 = o1.clone

  o1.object_id
  o1.hash

  o2.object_id
  o2.hash
end

# This should not crash the interpreter
class BadHash
  def hash
     "NOWAY"
  end
end

b = BadHash.new
{b => b}

# JRUBY-800: default initialize should not accept arguments
test_exception(ArgumentError) { Object.new(1) }
class NoArgClass
end
test_exception(ArgumentError) { NoArgClass.new(1) }

# JRUBY-980: public_methods, private_methods 
# and protected_methods should take a boolean arg
# to include or exclude inherited methods or not
class MethodFixture
  class << self
    def method; end
    public  
      def method0; end
    protected  
      def method1; end
  end
  def method2; end
  public
    def method3; end
  protected 
    def method4; end
  private
    def method5; end
end
test_equal(3, MethodFixture.singleton_methods(false).length)
m = MethodFixture.new
test_equal(3 + Object.new.methods.length, m.methods.length)
test_equal(2, m.public_methods(false).length)
test_equal(1, m.protected_methods(false).length)
test_equal(1, m.private_methods(false).length)

  
class Class1
  def initialize
    @one = 123
    @two = nil
  end
  def initialize_copy
  end
end

c1 = Class1.new

test_exception(NameError) do 
  c1.instance_variable_defined? :abc
end

test_ok !c1.instance_variable_defined?(:@three)
test_ok c1.instance_variable_defined?(:@one)
test_ok c1.instance_variable_defined?(:@two)

# JRUBY-2624: Object#initialize_copy should always be private
test_ok c1.private_methods.include?("initialize")
test_ok c1.private_methods.include?("initialize_copy")

# JRUBY-2247
test_equal ['now'], Time.methods.grep('now')
test_equal ["_load", "at", "gm", "local", "mktime", "now", "times", "utc"], Time.methods(false).sort
