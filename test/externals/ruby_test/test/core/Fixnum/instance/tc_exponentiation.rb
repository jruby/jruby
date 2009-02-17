###############################################################################
# tc_exponentiation.rb
#
# Test case for the Fixnum#** instance method.
###############################################################################
require 'test/unit'

class TC_Fixnum_Exponentiation_InstanceMethod < Test::Unit::TestCase
   def test_exponentiation_basic
      assert_respond_to(2, :**)
      assert_nothing_raised{ 2.**(1) }
      assert_kind_of(Fixnum, 2.**(1))
   end

   def test_exponentiation_pos
      assert_equal(8, 2**3)
      assert_equal(64, (2**3)**2)
      assert_equal(512, 2**(3**2))
   end

   # TODO: Figure out to how check a Rational value without require'ing the
   # rational library, which screws up other tests.
   def test_exponentiation_neg
      #assert_equal(Rational(1,8), 2**-3)
      assert_equal(-8, -2**3)
      assert_in_delta(-1.08005973889231, -2**3**-2, 0.001)
   end

   def test_exponentiation_one_and_zero
      assert_equal(0, 0**23)
      assert_equal(1, 23**0)
   end

   def test_exponentiation_expected_errors
      assert_raise(ArgumentError){ 2.send(:**, 1, 2) }
   end
end
