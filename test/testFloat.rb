require 'test/minirunit'
test_check "Test Floating points:"

# I had to choose a value for testing float equality
# 1.0e-11 maybe too big but since the result depends
# on the actual machine precision it may be too small
# too...
def test_float_equal(a,b)
  test_ok(a-b < 1.0e-11)
end

test_float_equal(9.2 , 3.5 + 5.7)
test_float_equal(9.9, 3.0*3.3)
test_float_equal(1.19047619 , 2.5 / 2.1)
test_float_equal(39.0625, 2.5 ** 4)

test_equal("1.1", 1.1.to_s)
test_equal("1.0", 1.0.to_s)
test_equal("-1.0", -1.0.to_s)

test_equal(nil, 0.0.infinite?)
test_equal(-1, (-1.0/0.0).infinite?)
test_equal(1, (+1.0/0.0).infinite?)
