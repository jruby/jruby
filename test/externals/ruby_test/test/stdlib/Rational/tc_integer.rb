######################################################################
# tc_integer.rb
#
# Test case for Integer methods defined within rational.rb.
######################################################################
require 'rational'
require 'test/unit'

class TC_Rational_Integer_Stdlib < Test::Unit::TestCase
   def setup
      @int1 = 27
      @int2 = 30
   end

   def test_gcd
      assert_respond_to(@int1, :gcd)
      assert_nothing_raised{ @int1.gcd(@int2) }
      assert_equal(3, @int1.gcd(@int2))
   end

   def test_gcd_expected_errors
      assert_raises(NoMethodError){ @int1.gcd("bogus") }
      assert_raises(ArgumentError){ @int1.gcd }
   end

   def test_gcd2
      assert_respond_to(@int1, :gcd)
      assert_nothing_raised{ @int1.gcd(@int2) }
      assert_equal(3, @int1.gcd(@int2))
   end

   def test_gcd2_expected_errors
      assert_raises(NoMethodError){ @int1.gcd2("bogus") }
#      assert_raises(ArgumentError){ @int1.gcd2 }
   end

   def test_lcm
      assert_respond_to(@int1, :lcm)
      assert_nothing_raised{ @int1.lcm(@int2) }
      assert_equal(270, @int1.lcm(@int2))
   end

   def test_lcm_expected_errors
      assert_raises(NoMethodError){ @int1.lcm("bogus") }
      assert_raises(ArgumentError){ @int1.lcm }
   end

   def test_gcdlcm
      assert_respond_to(@int1, :gcdlcm)
      assert_nothing_raised{ @int1.gcdlcm(@int2) }
      assert_equal([3,270], @int1.gcdlcm(@int2))
   end

   def test_gcdlcm_expected_errors
      assert_raises(NoMethodError){ @int1.gcdlcm("bogus") }
      assert_raises(ArgumentError){ @int1.gcdlcm }
   end

   def test_numerator
      assert_respond_to(@int1, :numerator)
      assert_nothing_raised{ @int1.numerator }
      assert_equal(27, @int1.numerator)
   end

   def test_denominator
      assert_respond_to(@int1, :denominator)
      assert_nothing_raised{ @int1.denominator }
      assert_equal(1, @int1.denominator)
   end

   def test_to_r
      assert_respond_to(@int1, :to_r)
      assert_nothing_raised{ @int1.to_r }
      assert_kind_of(Rational, @int1.to_r)
      assert_equal(Rational(27,1), @int1.to_r)
   end

   def teardown
      @int1 = nil
      @int2 = nil
   end
end
