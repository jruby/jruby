require 'test/unit'

class TestTimeAdd < Test::Unit::TestCase
  def test_add_many_under_ms
    t = Time.new(2000, 1, 1, 0, 0, 0)
    t += Rational(123456789, 1_000_000_000)
    assert_equal 123456789, t.nsec
    delta = Rational(999, 1000_000)
    1000.times do
      t += delta
    end
    # JRuby used to let NSec go above 10^6
    assert_equal [1, 122456789], [t.sec, t.nsec]
  end
end
