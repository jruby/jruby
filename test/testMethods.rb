require 'test/minirunit'
test_check "Test Methods"

def testMethod
    $toto = true
    "some output"
end

test_ok("some output" == testMethod)
test_ok($toto)
def testMethodWithArgs(a,b)
end
begin
	testMethodWithArgs()
	
rescue ArgumentError => boom
	test_ok(true)
end
begin
	testMethodWithArgs(1)
rescue ArgumentError => boom
	test_ok(true)
end

test_exception(ArgumentError) { testMethod(1, 2, 3) }

class MethodTestingOne
  def method_missing(name, *args)
    [name, args]
  end
end
mt1 = MethodTestingOne.new
test_equal([:hello, [1, 2]], mt1.hello(1, 2))


def f(x, &proc)
  proc.call()
end
y = "hello"
f(10) {
  test_equal("hello", y)
}

class TestMethods_X
  def hello
    "yeah"
  end
end
x = TestMethods_X.new
test_equal("TestMethods_X", "#{x.class}")
test_equal("yeah", "#{x.hello}")

p = Proc.new {|z| "#{z.size}"}
test_equal("hello".size.to_s, p.call("hello"))

class TM1
  define_method("foo") {
    $foo = true
  }
end

$foo = false
TM1.new.foo
test_ok($foo)

o = Object.new
def o.foo(x)
  "hello" + x
end
foo_method = o.method(:foo)
test_equal(1, foo_method.arity)
foo_proc = foo_method.to_proc
test_equal(Proc, foo_proc.class)
test_equal("helloworld", foo_proc.call("world"))

class TM_A
  def foo
    "x"
  end
  def bar
    "y"
  end
end
class TM_B < TM_A
  def foo
    super
  end
  def bar
    super()
  end
end
test_equal("x", TM_B.new.foo) # Tests ZSuperNode
test_equal("y", TM_B.new.bar) # Tests SuperNode

def no_arg_opt(*); end
test_no_exception { no_arg_opt }
