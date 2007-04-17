require 'test/minirunit'

class MyArray < Array
end

arr = MyArray.new([1,2,3])
arr2 = MyArray.new([[1,2],[2,3],[3,3]])

test_equal(Array, arr2.transpose.class)
test_equal(MyArray, arr.compact.class)
test_equal(MyArray, arr.reverse.class)
test_equal(MyArray, arr2.flatten.class)
test_equal(MyArray, arr.uniq.class)
test_equal(MyArray, arr.sort.class)
test_equal(MyArray, arr[1,2].class)
test_equal(MyArray, arr[1..2].class)
test_equal(Array, arr.to_a.class)
test_equal(MyArray, arr.to_ary.class)
test_equal(MyArray, arr.slice(1,2).class)
test_equal(MyArray, arr.slice!(1,2).class)
test_equal(MyArray, (arr*0).class)
test_equal(MyArray, (arr*2).class)
test_equal(MyArray, arr.replace([1,2,3]).class)
test_equal(Array, arr.last(2).class)
test_equal(Array, arr.first(2).class)
test_equal(Array, arr.collect.class)
test_equal(Array, arr.collect{true}.class)
test_equal(Array, arr.zip([1,2,3]).class)
test_equal(MyArray, arr.dup.class)
