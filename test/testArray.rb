require 'minirunit'
test_check "Test Array:"
arr = ["zero", "first"]

arr.unshift "second", "third"


test_ok(["third", "zero", "first"] == arr[1..4])
test_ok('["third", "zero", "first"]' == arr[1..4].inspect)

arr << "fourth"

test_ok("fourth" == arr.pop());
test_ok("second" == arr.shift());

test_ok(Array == ["zero", "first"].type)
test_ok("Array" == Array.to_s)
Java::import "org.jruby.test"
array = TestHelper::createArray(4)
array.each {		# this should not generate an exception
	|test|
	true
}
test_ok(true)		#this is always true but it is used to count the iteration on ARGV as a test
test_equal(array.length,  4)
