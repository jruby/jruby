require 'minirunit'
test_check "Test Array:"
arr = ["zero", "first"]

arr.unshift "second", "third"
test_equal(["second", "third", "zero", "first"], arr)
test_equal(["first"], arr[-1..-1])
test_equal(["first"], arr[3..3])
test_equal([], arr[3..2])
test_equal(nil, arr[3..1])
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


test_equal([1,2,3,4], [[[1], 2], [3, [4]]].flatten)
test_equal(nil, [].flatten!)

#arr = []
#arr << [[[arr]]]
#test_exception(ArgumentError) {
#  arr.flatten
#}
#test_exception(ArgumentError) {
#  arr.flatten!
#}
