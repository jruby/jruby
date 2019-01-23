require 'test/unit'

# Test that core class methods are correctly raising errors for incorrect call
# arities.
class TestFormat < Test::Unit::TestCase
  # Test Case based on https://github.com/jruby/jruby/issues/5556
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
end

