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

  def test_cmp_non_comparables
    assert $!.nil?, "$! not nil but: #{$!.inspect}"
    # should not raise errors :
    assert_equal nil, 0 <=> Time.now
    assert $!.nil?, "$! not nil but: #{$!.inspect}"
    assert_equal nil, nil <=> 42 if RUBY_VERSION > '1.9'
    # 1.8.7 NoMethodError: undefined method `<=>' for nil:NilClass
    assert $!.nil?, "$! not nil but: #{$!.inspect}"
    assert_equal nil, '42' <=> nil
    assert $!.nil?, "$! not nil but: #{$!.inspect}"
    assert_equal nil, Object.new <=> nil if RUBY_VERSION > '1.9'
    # 1.8.7 NoMethodError: undefined method `<=>' for #<Object:0xceb431e>
    assert $!.nil?, "$! not nil but: #{$!.inspect}"

    require 'bigdecimal'
    big = BigDecimal.new '-10000000000'
    assert_equal nil, big <=> Time.now
    assert $!.nil?, "$! not nil but: #{$!.inspect}"
    assert_equal nil, big <=> nil
    assert $!.nil?, "$! not nil but: #{$!.inspect}"
    assert_equal nil, big <=> Object.new
    assert $!.nil?, "$! not nil but: #{$!.inspect}"

    # Symbol only includes Comparable on 1.9
    assert_equal nil, :sym <=> nil if RUBY_VERSION > '1.9'
    assert $!.nil?, "$! not nil but: #{$!.inspect}"
    assert_equal nil, :sym <=> 42 if RUBY_VERSION > '1.9'
    assert $!.nil?, "$! not nil but: #{$!.inspect}"
    assert_equal 0,   :sym <=> :sym if RUBY_VERSION > '1.9'
    assert $!.nil?, "$! not nil but: #{$!.inspect}"
    assert (:zym <=> :sym) > 0 if RUBY_VERSION > '1.9'
    assert $!.nil?, "$! not nil but: #{$!.inspect}"
  end

  def test_compareTo_non_comparables
    assert $!.nil?, "$! not nil but: #{$!.inspect}"
    # should not raise errors :
    # TODO: RubyFixnum#compareTo breaks the contract of not-throwing
    # with: TypeError: can't convert nil into Integer
    #assert_equal 0, invokeCompareTo(0, Time.now)
    assert $!.nil?, "$! not nil but: #{$!.inspect}"
    assert_equal 0, invokeCompareTo(nil, 42)
    assert $!.nil?, "$! not nil but: #{$!.inspect}"
    assert_equal 0, invokeCompareTo(Object.new, nil)
    assert $!.nil?, "$! not nil but: #{$!.inspect}"

    require 'bigdecimal'
    big = BigDecimal.new '-10000000000'
    assert_equal 0, invokeCompareTo(big, Time.now)
    assert $!.nil?, "$! not nil but: #{$!.inspect}"
    assert_equal 0, invokeCompareTo(big, Object.new)
    assert $!.nil?, "$! not nil but: #{$!.inspect}"

    assert_equal 0, invokeCompareTo(:sym, nil)
    assert $!.nil?, "$! not nil but: #{$!.inspect}"
    assert_equal 0, invokeCompareTo(:sym, 42)
    assert $!.nil?, "$! not nil but: #{$!.inspect}"
    assert_equal 0, invokeCompareTo(:sym, :sym)
    assert $!.nil?, "$! not nil but: #{$!.inspect}"
    assert invokeCompareTo(:zym, :sym) > 0
    assert $!.nil?, "$! not nil but: #{$!.inspect}"
  end

  private

  def invokeCompareTo(o1, o2)
    Java::DefaultPackageClass.compareTo(o1, o2)
  end

end