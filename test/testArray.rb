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

