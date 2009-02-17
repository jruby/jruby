#####################################################################
# tc_modulo.rb
#
# Test case for the Fixnum#% instance method.
#####################################################################
require 'test/unit'

class TC_Fixnum_Modulo_InstanceMethod < Test::Unit::TestCase
   def setup
      @num_pos = 42
      @num_neg = -37
   end

   def test_modulo_basic
      assert_respond_to(@num_pos, :%)
      assert_nothing_raised{ @num_pos.%(1) }
      assert_nothing_raised{ @num_pos % 1 }
   end

   def test_modulo_pos
      assert_equal(0, @num_pos % 1)
      assert_equal(2, @num_pos % 5 % 3)
      assert_equal(-3, @num_pos % -5)
      assert_equal(-1, @num_pos % 5 % -3)
   end

   def test_modulo_neg
      assert_equal(0, @num_neg % 1)
      assert_equal(1, @num_neg % 10 % 2)
      assert_equal(-7, @num_neg % -10)
      assert_equal(-1, @num_neg % 10 % -2)
   end

   def test_modulo_one
      assert_equal(0, 0 % 1)
      assert_equal(0, 1 % 1)
   end

   def test_modulo_expected_errors
      assert_raise(ArgumentError){ @num_pos.send(:/, 1, 2) }
      assert_raise(ZeroDivisionError){ @num_pos % 0 }
   end

   def teardown
      @num_pos = nil
      @num_neg = nil
   end
end
