require 'test/minirunit'
test_check "Test Struct"

s = Struct.new("MyStruct", :x, :y)

test_equal(Struct::MyStruct, s)

s1 = s.new(11, 22)
test_equal(11, s1["x"])
test_equal(22, s1["y"])

s1 = s.new
test_equal(nil, s1.x)
test_equal(nil, s1.y)
s1.x = 10
s1.y = 20
test_equal(10, s1.x)
test_equal(20, s1.y)
test_equal(10, s1[0])
test_equal(20, s1[1])
test_equal(20, s1[-1])

test_equal(2, s1.length)
test_equal(2, s1.size)

test_equal([10, 20], s1.values)

test_exception(NameError) { s1["froboz"] }
test_exception(IndexError) { s1[10] }

s2 = s.new(1, 2)
test_equal(1, s2.x)
test_equal(2, s2.y)

test_exception(ArgumentError) { s.new(1,2,3,4,5,6,7) }

# Anonymous Struct
a = Struct.new(:x, :y)
a1 = a.new(5, 7)
test_equal(5, a1.x)
