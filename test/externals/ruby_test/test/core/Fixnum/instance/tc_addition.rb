#####################################################################
# tc_addition.rb
#
# Test case for the Fixnum#+ instance method.
#####################################################################
require 'test/unit'

class TC_Fixnum_Addition_InstanceMethod < Test::Unit::TestCase
   # Assumes 8 bit byte, 1 bit flag, and 2's comp
   def setup
      @num1 = 1
      @max  = 2**(1.size * 8 - 2) - 1
      @min  = -@max - 1
   end

   def test_addition_basic
      assert_respond_to(@num1, :+)
      assert_nothing_raised{ @num1.+(1) }
      assert_nothing_raised{ @num1 + 1 }
   end

   def test_addition
      assert_equal(@min, @min + 0)
      assert_equal(@min, 0 + @min)
      assert_equal(true, @min + 1 > @min)
      # Fails
#      assert_equal(true, @min + 0.1 > @min)
   end

   def test_addition_expected_errors
      assert_raise(ArgumentError){ @num1.send(:+, 1, 2) }
   end

   def teardown
      @num1 = nil
      @max  = nil
      @min  = nil
   end
end
