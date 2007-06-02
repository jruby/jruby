######################################################################
# tc_divmod.rb
#
# Test case for the Numeric#divmod instance method.
######################################################################
require 'test/unit'

class TC_Numeric_Divmod_InstanceMethod < Test::Unit::TestCase
   def setup
      @num_int = 13
      @den_int = 4
      @num_flt = 13.0
      @den_flt = 4.0
      @num_bigint = 2**33
      @den_bigint = 2**32
   end

   def test_divmod_basic
      assert_respond_to(@num_int, :divmod)
      assert_nothing_raised{ @num_int.divmod(@den_int) }
      assert_kind_of(Array, @num_int.divmod(@den_int))
   end

   def test_divmod_integers
      assert_equal([3, 1], @num_int.divmod(@den_int))
      assert_equal([0, 4], @den_int.divmod(@num_int))
   end

   def test_divmod_integer_and_float
      assert_equal([3, 1], @num_int.divmod(@den_flt))
      assert_equal([0, 4], @den_flt.divmod(@num_int))
   end

   def test_divmod_floats
      assert_equal([3.0, 1.0], @num_flt.divmod(@den_flt))
      assert_equal([0.0, 4.0], @den_flt.divmod(@num_flt))
   end

   def test_divmod_bignum_integers
      assert_equal([2, 0], @num_bigint.divmod(@den_bigint))
   end

   def test_divmod_expected_errors
      assert_raises(ArgumentError){ @num_int.divmod }
      assert_raises(ZeroDivisionError){ @num_int.divmod(0) }
      assert_raises(TypeError){ @num_int.divmod(nil) }
      assert_raises(TypeError){ @num_int.divmod('test') }
      assert_raises(TypeError){ @num_int.divmod(true) }
   end

   def teardown
      @num_int = nil
      @den_int = nil
   end
end
