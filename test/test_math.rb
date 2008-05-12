require 'test/unit'

class TestMath < Test::Unit::TestCase
   # JRUBY-1591
   def test_tanh_returns_1_when_given_1_over_0
       assert_equal(1.0, Math.tanh(1.0/0.0))
   end
end
