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
