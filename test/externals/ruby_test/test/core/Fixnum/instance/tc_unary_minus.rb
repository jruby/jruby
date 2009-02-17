###############################################################################
# tc_unary_minus.rb
#
# Test case for the Fixnum#-@ instance method.
###############################################################################
require 'test/unit'

class TC_Fixnum_UnaryMinus_InstanceMethod < Test::Unit::TestCase
   def test_unary_minus_basic
      assert_respond_to(2, :-@)
      assert_nothing_raised{ 2.-@ }
      assert_kind_of(Fixnum, 2.-@)
   end

   def test_unary_minus
      assert_equal(-2, 2.-@)
      assert_equal(2, -2.-@)
      assert_equal(0, 0.-@)
   end

   def test_unary_minus_expected_errors
      assert_raise(ArgumentError){ 2.send(:-@, 1) }
   end
end
