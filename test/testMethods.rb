require 'minirunit'
test_check "Test Methods:"

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
