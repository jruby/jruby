require 'test/unit'

class TestTimeSubtract < Test::Unit::TestCase
  def test_subtract_decimal_length
    time1 = Time.utc(2000, 1, 2, 23, 59, 59, Rational(999999999, 1000))
    time2 = Time.utc(2000, 1, 2, 0, 0, 0, Rational(1, 1000))

    assert_equal 86_399.999999998, time1 - time2
  end
end
