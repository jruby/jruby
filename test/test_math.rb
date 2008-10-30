require 'test/unit'

class TestMath < Test::Unit::TestCase
   # JRUBY-1591
   def test_tanh_returns_1_when_given_1_over_0
       assert_equal(1.0, Math.tanh(1.0/0.0))
   end

   def test_frexp_inf
      inf = 1.0 / 0
      assert_nothing_raised{ Math.frexp(inf) }
      assert_equal(inf, Math.frexp(inf).first)
      assert_equal(0, Math.frexp(inf).last)
   end

   def test_frexp_nan
      nan = 0.0 / 0
      assert_nothing_raised{ Math.frexp(nan) }
      assert(Math.frexp(nan).first.nan?)
      assert_equal(0, Math.frexp(nan).last)
   end
end
