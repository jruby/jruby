$: << File.dirname($0) << File.join(File.dirname($0), "..")
require 'rubicon'


class TestNumeric < Rubicon::TestCase

  def test_UPLUS
    skipping("untestable")
  end

  def test_UMINUS
    skipping("untestable")
  end

  def test_VERY_EQUAL # '==='  
    assert(1.0 === 1)
    assert(1   === 1.0)
    assert(1.1 === 1.1)
  end

  def test_abs
    skipping("untestable")
  end

  def test_coerce
    assert_equal([2.5, 1.0], 1.coerce(2.5))
    assert_equal([3.0, 1.2], 1.2.coerce(3))
    assert_equal([2, 1],     1.coerce(2))
  end

  def test_divmod
    skipping("untestable")
  end

  def test_eql?
    assert(1.eql?(1))
    assert(1.0.eql?(1.0))
    assert(!(1.eql?(1.0)))
    assert(!(1.0.eql?(1)))
  end

  def test_integer?
    assert(1.integer?)
    assert((10**50).integer?)
    assert(!1.2.integer?)
  end

  def test_nonzero?
    assert_equal(nil,    0.nonzero?)
    assert_equal(1,      1.nonzero?)
    assert_equal(10**50, (10**50).nonzero?)
    assert_equal(1.2,    1.2.nonzero?)
  end

  def test_zero?
    assert_equal(true, 0.zero?)
    assert_equal(true, 0.0.zero?)
    assert_equal(false, 1.zero?)
    assert_equal(false, 1.1.zero?)
  end

end

Rubicon::handleTests(TestNumeric) if $0 == __FILE__
