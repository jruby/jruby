######################################################################
# tc_divmod.rb
#
# Test case for the Bignum#divmod instance method.
######################################################################
require 'test/unit'

class TC_Bignum_Divmod_InstanceMethod < Test::Unit::TestCase
   def setup
      @num_int = 9223372036854775808
      @den_int = 8589934593
      @num_flt = 9223372036854775808.0
      @den_flt = 8589934593.0
   end

   def test_divmod_basic
      assert_respond_to(@num_int, :divmod)
      assert_nothing_raised{ @num_int.divmod(@den_int) }
      assert_kind_of(Array, @num_int.divmod(@den_int))
   end

   def test_divmod_integers
      assert_equal([1073741823, 7516192769.0], @num_int.divmod(@den_int))
      assert_equal([0, 8589934593], @den_int.divmod(@num_int))
   end

   def test_divmod_integer_and_float
      assert_equal([0.0, 8589934593.0], @den_flt.divmod(@num_int))
      assert_equal([1073741823, 7516192769.0], @num_int.divmod(@den_flt))
   end

   def test_divmod_floats
      assert_equal([1073741823, 7516192769.0], @num_flt.divmod(@den_flt))
      assert_equal([0.0, 8589934593.0], @den_flt.divmod(@num_flt))
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
      @num_flt = nil
      @den_flt = nil
   end
end
