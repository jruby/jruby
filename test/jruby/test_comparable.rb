require 'test/unit'

class TestComparable < Test::Unit::TestCase
  class Smallest
    include Comparable
    def <=> (other)
      -1
    end
  end

  class Equal
    include Comparable
    def <=> (other)
      0
    end
  end

  class Biggest
    include Comparable
    def <=> (other)
      1
    end
  end

  class Different
    include Comparable
    def <=> (other)
      nil
    end
  end

  def setup
    @s = Smallest.new
    @e = Equal.new
    @b = Biggest.new
    @d = Different.new
  end

  def test_spaceship
    assert_equal(-1, @s <=> 3)
    assert_equal(-1, @s <=> 1)
    assert_equal(0, @e <=> 3)
    assert_equal(0, @e <=> 1)
    assert_equal(1, @b <=> 3)
    assert_equal(1, @b <=> 1)
    assert_equal(nil, @d <=> 3)
    assert_equal(nil, @d <=> 1)
  end

  def test_smaller
    assert_equal(true,  @s < 3)
    assert_equal(true,  @s <= 3)
    assert_equal(false, @s > 3)
    assert_equal(false, @s >= 3)
    assert_equal(false, @s == 3)
    assert_equal(false, @s.between?(1, 3))
  end

  def test_equal
    assert_equal(false, @e < 3)
    assert_equal(true,  @e <= 3)
    assert_equal(false, @e > 3)
    assert_equal(true, @e >= 3)
    assert_equal(true, @e == 3)
    assert_equal(true, @e.between?(1, 3))
  end

  def test_bigger
    assert_equal(false, @b < 3)
    assert_equal(false, @b <= 3)
    assert_equal(true,  @b > 3)
    assert_equal(true,  @b >= 3)
    assert_equal(false, @b == 3)
    assert_equal(false, @b.between?(1, 3))
  end

  def test_different
    assert_raises(ArgumentError) { @d < 3 }
    assert_raises(ArgumentError) { @d <= 3 }
    assert_raises(ArgumentError) { @d > 3 }
    assert_raises(ArgumentError) { @d >= 3 }
    assert(@d != 3)
    assert_raises(ArgumentError) { @d.between?(1, 3) }
  end
end