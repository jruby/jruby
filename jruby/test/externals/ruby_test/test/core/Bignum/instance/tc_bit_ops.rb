##########################################################
# tc_bit_ops.rb
#
# Test case for Bignum bit operations ~, |, &, ^, <<, >>
##########################################################
require 'test/unit'

class TC_Bignum_Bit_Ops < Test::Unit::TestCase
   def setup
      @bignum1 = 18446744073709551616 # 2**64
      @bignum2 = 36893488147419103232 # 2**65
   end

   def test_invert
      assert_respond_to(@bignum1, :~)

      assert_nothing_raised{ ~@bignum1 }
      assert_nothing_raised{ ~@bignum2 }

      assert_equal(-18446744073709551617, ~@bignum1)
      assert_equal(-36893488147419103233, ~@bignum2)
   end 

   def test_bitwise_or
      assert_respond_to(@bignum1, :|)

      assert_nothing_raised{ @bignum1 | @bignum2 }
      assert_nothing_raised{ @bignum2 | @bignum1 }

      assert_equal(18446744073709551616, @bignum1 | 0)
      assert_equal(18446744073709551617, @bignum1 | 1)
      assert_equal(-1, @bignum1 | -1)
   end

   def test_bitwise_and
      assert_respond_to(@bignum1, :&)

      assert_nothing_raised{ @bignum1 & @bignum2 }
      assert_nothing_raised{ @bignum2 & @bignum1 }

      assert_equal(0, @bignum1 & 1)
      assert_equal(18446744073709551616, @bignum1 & -1)
      assert_equal(36893488147419103232, @bignum2 & -1)
      assert_equal(18446744073709551616, @bignum1 & @bignum1)
   end

   def test_bitwise_exclusive_or
      assert_respond_to(@bignum1, :^)

      assert_nothing_raised{ @bignum1 ^ @bignum2 }
      assert_nothing_raised{ @bignum2 ^ @bignum1 }

      assert_equal(18446744073709551616, @bignum1 ^ 0)
      assert_equal(18446744073709551617, @bignum1 ^ 1)
      assert_equal(-18446744073709551617, @bignum1 ^ -1)
   end

   def test_left_shift
      assert_respond_to(@bignum1, :<<)

      assert_nothing_raised{ @bignum1 << 2 }
      assert_nothing_raised{ @bignum1 << 0 }
      assert_nothing_raised{ @bignum1 << -2 }

      assert_equal(18446744073709551616, @bignum1 << 0)
      assert_equal(36893488147419103232, @bignum1 << 1)
   end

   def test_right_shift
      assert_respond_to(@bignum1, :>>)

      assert_nothing_raised{ @bignum1 >> 2 }
      assert_nothing_raised{ @bignum1 >> 0 }
      assert_nothing_raised{ @bignum1 >> -2 }

      assert_equal(18446744073709551616, @bignum1 >> 0)
      assert_equal(9223372036854775808, @bignum1 >> 1)
   end

   # This test was added as the result of ruby-core:9020.
   #
   def test_right_shift_alignment
      assert_equal(-1, (1 - 2**31) >> 31)
      assert_equal(-1, (1 - 2**32) >> 32)
      assert_equal(-1, (1 - 2**33) >> 33)
      assert_equal(-1, (1 - 2**63) >> 63)
      assert_equal(-1, (1 - 2**64) >> 64)
      assert_equal(-1, (1 - 2**65) >> 65)
   end

   def teardown
      @bignum = nil
   end
end
