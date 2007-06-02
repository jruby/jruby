require 'test/unit'


class TestComparable < Test::Unit::TestCase

  class C
    include Comparable
    attr :val
    def initialize(val)
      @val = val
    end
    def <=>(other)
      @val <=> other.val
    end
  end

  def setup
    @a = C.new(1)
    @b = C.new(2)
    @c = C.new(1)
    @d = C.new(3)
  end

  def test_00_sanity
    assert_equal( 0, @a <=> @a)
    assert_equal( 0, @a <=> @c)
    assert_equal(-1, @a <=> @b)
    assert_equal( 1, @b <=> @a)
  end

  def test_EQUAL # '=='
    assert(  @a == @a)
    assert(  @a == @c)
    assert(!(@a == @b))
    assert(  @a != @b)
  end

  def test_GE # '>='
    assert(!(@a >= @b))
    assert( (@a >= @a))
    assert( (@b >= @a))
  end

  def test_GT # '>'
    assert(!(@a > @b))
    assert(!(@a > @a))
    assert( (@b > @a))
  end

  def test_LE # '<='
    assert( (@a <= @b))
    assert( (@a <= @a))
    assert(!(@b <= @a))
  end

  def test_LT # '<'
    assert( (@a < @b))
    assert(!(@a < @a))
    assert(!(@b < @a))
  end

  def test_between?
    assert( @a.between?(@a, @c))
    assert(!@a.between?(@b, @c))
    assert( @b.between?(@a, @b))
    assert( @b.between?(@a, @d))
    assert(!@d.between?(@a, @b))
    assert(!@d.between?(@b, @b))
  end

end
