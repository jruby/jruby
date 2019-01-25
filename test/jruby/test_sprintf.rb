require 'test/unit'

# Test Case based on https://github.com/jruby/jruby/issues/5556
class TestFormat < Test::Unit::TestCase
  
  def test_sprintf_one
    n = 97.65625
    s = format("%f %f %.1f", n, n.round(1), n).to_s
    assert(s === "97.656250 97.700000 97.7", "Test failed, value of #{s} did not match '97.656250 97.700000 97.7'.")
  end
  
  def test_sprintf_two
    n = 1234567892.0
    s = format("%01.0f", n).to_s
    assert(s === "1234567892", "Test failed, value of #{s} did not match '1234567892'.")
  end
#  
  def test_sprintf_three
    n = 5.005
    s = format("%.2f",n)
    assert(s === "5.00", "Test failed, value of #{s} did not match '5.00'.")
  end
end

