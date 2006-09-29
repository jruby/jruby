require 'test/minirunit'
test_check "Test Comparable:"

class Smallest
  include Comparable
  def <=> (other)
    -1
  end
end
s = Smallest.new

class Equal
  include Comparable
  def <=> (other)
    0
  end
end
e = Equal.new


class Biggest
  include Comparable
  def <=> (other)
    1
  end
end
b = Biggest.new

class Different
  include Comparable
  def <=> (other)
    nil
  end
end
d = Different.new

test_equal(-1, s <=> 3)
test_equal(-1, s <=> 1)
test_equal(0, e <=> 3)
test_equal(0, e <=> 1)
test_equal(1, b <=> 3)
test_equal(1, b <=> 1)
test_equal(nil, d <=> 3)
test_equal(nil, d <=> 1)

test_equal(true, s < 3)
test_equal(true, s <= 3)
test_equal(false, s > 3)
test_equal(false, s >= 3)
test_equal(false, s == 3)
test_equal(false, s.between?(1, 3))

test_equal(false, e < 3)
test_equal(true, e <= 3)
test_equal(false, e > 3)
test_equal(true, e >= 3)
test_equal(true, e == 3)
test_equal(true, e.between?(1, 3))

test_equal(false, b < 3)
test_equal(false, b <= 3)
test_equal(true, b > 3)
test_equal(true, b >= 3)
test_equal(false, b == 3)
test_equal(false, b.between?(1, 3))

test_exception(ArgumentError) { d < 3 }
test_exception(ArgumentError) { d <= 3 }
test_exception(ArgumentError) { d > 3 }
test_exception(ArgumentError) { d >= 3 }
test_equal(nil, d == 3)
test_exception(ArgumentError) { d.between?(1, 3) }

