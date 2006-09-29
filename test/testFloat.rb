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


# copied from ruby/sample/test.rb in ruby distribution
test_ok(2.6.floor == 2)
test_ok((-2.6).floor == -3)
test_ok(2.6.ceil == 3)
test_ok((-2.6).ceil == -2)
test_ok(2.6.truncate == 2)
test_ok((-2.6).truncate == -2)
test_ok(2.6.round == 3)
test_ok((-2.4).truncate == -2)
test_ok((13.4 % 1 - 0.4).abs < 0.0001)
nan = 0.0/0
def nan_test(x,y)
  test_ok(x != y)
  test_ok((x < y) == false)
  test_ok((x > y) == false)
  test_ok((x <= y) == false)
  test_ok((x >= y) == false)
end
nan_test(nan, nan)
nan_test(nan, 0)
nan_test(nan, 1)
nan_test(nan, -1)
nan_test(nan, 1000)
nan_test(nan, -1000)
nan_test(nan, 1_000_000_000_000)
nan_test(nan, -1_000_000_000_000)
nan_test(nan, 100.0);
nan_test(nan, -100.0);
nan_test(nan, 0.001);
nan_test(nan, -0.001);
nan_test(nan, 1.0/0);
nan_test(nan, -1.0/0);

#s = "3.7517675036461267e+17"
#test_ok(s == sprintf("%.16e", s.to_f))
f = 3.7517675036461267e+17
test_ok(f == sprintf("%.16e", f).to_f)

# a few added tests for MRI compatibility now that we override relops
# from Comparable

class Smallest
  include Comparable
  def <=> (other)
    -1
  end
end

class Different
  include Comparable
  def <=> (other)
    nil
  end
end

s = Smallest.new
d = Different.new
test_equal(nil, 3.0 <=> s)
test_equal(nil, 3.0 <=> d)
test_exception(ArgumentError) { 3.0 < s }
test_exception(ArgumentError) { 3.0 < d }
