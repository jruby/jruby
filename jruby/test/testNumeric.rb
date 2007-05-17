require 'test/minirunit'
test_check "Test numerics:"

a = Numeric.new
b = Numeric.new

test_equal(Array,(a.coerce b).class)

test_equal(a,+a)
test_exception(TypeError){-a}

test_equal(0,a <=> a)
test_equal(nil,a <=> b)
test_equal(nil,a <=> 1)
test_equal(nil,a <=> "")

test_exception(NoMethodError){a.quo b} # no '%' method

test_equal(false,a.eql?(b))

[:div,:divmod,:modulo,:remainder].each do |meth|
    test_exception(NoMethodError){a.send meth,b}
end

test_exception(ArgumentError){a.abs}

test_exception(NoMethodError){a.to_int}

test_equal(false,a.integer?)
test_equal(false,a.zero?)
test_equal(a,a.nonzero?)

[:floor,:ceil,:round,:truncate].each do |meth|
    test_exception(TypeError){a.send meth}
end

test_exception(ArgumentError){a.step(0)}


test_equal(nil,a == b)
test_equal(true,a == a)
test_exception(ArgumentError){a < b}
test_exception(NoMethodError){a + b}


# Fixnum

a = 0

10.step(20) do |i|
    a+=i
end

test_equal(a,165)

a = 0
10.step(20,3) do |i|
    a+=i
end

test_equal(a,58)

a = 0

20.step(10,-3) do |i|
    a+=i
end

test_equal(a,62)

# Float
a = 0.0

10.0.step(12.0) do |i|
    a+=i
end

test_equal(a,33.0)

a = 0.0
10.0.step(12.0,0.3) do |i|
    a+=i
end

test_equal(a,76.3)

a = 0.0
12.0.step(10.0,-0.3) do |i|
    a+=i
end

test_equal(a,77.7)
