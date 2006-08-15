require 'test/minirunit'
test_check "Test super:"

# Most of these tests are not actual tests, but merely code which does not die
# TODO: These could be expanded to make sure the right supers are being called and also they can
# be expanded to include self and superclass tests.

# Test 1: A mixin can call super and not die trying
module MixinTest1
  private

  def initialize(*args)
    super
  end
end

class Test1
  include MixinTest1
end

Test1.new

# Test 2: Super class base class super should work
class Test2Base
  def initialize
	super
  end
end

class Test2 < Test2Base
end

Test2.new

# Test 3: Singleton class should be able to call super

module SingletonTest3
end

class << SingletonTest3
  def append_features(feature)
    super
  end
end

class Test3
  include SingletonTest3
end

Test3.new

# Test 4: Super working with aliases

class Base1Test4
  def foo
    "good"
  end
  alias bar foo
  def baz
    "bad"
  end
end

class Base2Test4 < Base1Test4
end

class Base3Test4 < Base2Test4
  def foo
    super
  end
  def bar
    super
  end

  alias baz bar
end

f = Base3Test4.new
test_equal("good", f.foo)
test_equal("good", f.bar)
test_equal("good", f.baz)

# Test5: explicitly defined new can call super
class BaseTest5
  def initialize(string, *args)
  end
end

class Test5 < BaseTest5
  def self.new(string, *args)
    super(string, *args)
  end
end

Test5.new("HEH")

# Test6: class method can call super
class BaseTest6
  def self.x
  end
end

class Test6 < BaseTest6
  def self.x
    super
  end
end

Test6.x

# Test7: Another alias super test
class Base1Test7
  def foo; "foo" end
end
class Base2Test7 < Base1Test7
  alias bar foo
  def foo; "foo+" + super end
end
class Test7 < Base2Test7
  alias baz foo
  undef foo
end

x = Test7.new
test_equal("foo+foo", x.baz)

# Test 8: 
class Object
  def require(file, *extras)
    super(file, *extras)
  end
end

require 'test/superReq1'

class Mongo
  require 'test/superReq2'
end


# ZSuper tests

class Test9Base
  def foo(a = 'bad')
    test_equal("good", a)
  end

  def gar(a, b="bad", *rest)
  	test_equal(2, b)
  	test_equal([3, 4], rest)
  end

  def har(a="bad", b="bad", c="bad")
    test_equal("good", a)
    test_equal("good", b)
    test_equal("bad", c)
  end
end

class Test9Derived < Test9Base
  def foo(a = 'good')
    super
  end  

  def bar(a, b=100, *rest)
  end
  
  def gar(a, b="good", *rest)
    super
  end

  def har(a, b)
    super
  end
end

Test9Derived.new.foo
Test9Derived.new.bar(1,2,3,4)
Test9Derived.new.gar(1,2,3,4)
Test9Derived.new.har("good", "good")

# taken from MRI test/ruby/test_super.rb

class Base
  def single(a) a end
  def double(a, b) [a,b] end
  def array(*a) a end
  def optional(a = 0) a end
end
class Single1 < Base
  def single(*) super end
end
class Single2 < Base
  def single(a,*) super end
end
class Double1 < Base
  def double(*) super end
end
class Double2 < Base
  def double(a,*) super end
end
class Double3 < Base
  def double(a,b,*) super end
end
class Array1 < Base
  def array(*) super end
end
class Array2 < Base
  def array(a,*) super end
end
class Array3 < Base
  def array(a,b,*) super end
end
class Array4 < Base
  def array(a,b,c,*) super end
end
class Optional1 < Base
  def optional(a = 1) super end
end
class Optional2 < Base
  def optional(a, b = 1) super end
end
class Optional3 < Base
  def single(a = 1) super end
end


test_equal(1, Single1.new.single(1))
test_equal(1, Single2.new.single(1))

test_equal([1, 2], Double1.new.double(1, 2))
test_equal([1, 2], Double2.new.double(1, 2))
test_equal([1, 2], Double3.new.double(1, 2))

test_equal([], Array1.new.array())
test_equal([1], Array1.new.array(1))
test_equal([1], Array2.new.array(1))
test_equal([1,2], Array2.new.array(1, 2))
test_equal([1,2], Array3.new.array(1, 2))
test_equal([1,2,3], Array3.new.array(1, 2, 3))
test_equal([1,2,3], Array4.new.array(1, 2, 3))
test_equal([1,2,3,4], Array4.new.array(1, 2, 3, 4))

test_equal(9, Optional1.new.optional(9))
test_equal(1, Optional1.new.optional)

test_exception(ArgumentError) do
    # call Base#optional with 2 arguments; the 2nd arg is supplied
    test_equal(9, Optional2.new.optional(9))
  end

test_exception(ArgumentError) do
    # call Base#optional with 2 arguments
    test_equal(9, Optional2.new.optional(9, 2))
  end

test_equal(9, Optional3.new.single(9))
  # call Base#single with 1 argument; the arg is supplied
test_equal(1, Optional3.new.single)


