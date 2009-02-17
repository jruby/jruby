#####################################################################
# tc_zero.rb
#
# Test case for the Numeric#zero? instance method.
#####################################################################
require 'test/unit'

class TC_Numeric_Zero_InstanceMethod < Test::Unit::TestCase
   def setup
      @zero     = 0
      @nonzero  = 1
      @float_z  = 0.0
      @float_nz = 0.00001
   end
   
   def test_zero_basic
      assert_respond_to(@zero, :zero?)
      assert_nothing_raised{ @zero.zero? }
   end
   
   def test_zero_integers
      assert_equal(true, @zero.zero?)
      assert_equal(false, @nonzero.zero?)
   end

   def test_zero_floats
      assert_equal(true, @float_z.zero?)
      assert_equal(false, @float_nz.zero?)
   end

   def test_zero_unary_minus
      assert_equal(true, -0.zero?)
      assert_equal(true, -0.0.zero?)
   end

   def test_zero_expected_errors
      assert_raise(ArgumentError){ @zero.zero?(true) }
   end
   
   def teardown
      @zero     = nil
      @nonzero  = nil
      @float_z  = nil
      @float_nz = nil
   end
end
