require 'test/unit'

require 'mathn'

class TestMathn < Test::Unit::TestCase
  def test_complex_with_0_imaginary_part_is_fixnum
    assert_equal(Fixnum, Complex(1,0).class)
  end

  def test_rational_with_0_remainder_is_fixnum
    assert_equal(Fixnum, Rational(42,7).class)
  end
end