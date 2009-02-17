##########################################################################
# tc_arithmetic_ops.rb
#
# Test case for Bignum arithmetic operations +, -, *, /, %, ** and # -@.
# This also includes tests for the Bignum#div and Bignum#module aliases.
##########################################################################
require 'test/unit'

class TC_Bignum_Arithmetic_Ops < Test::Unit::TestCase
   def setup
      @bignum1 = 18446744073709551616 # 2**64
      @bignum2 = 36893488147419103232 # 2**65
   end

   def test_addition
      assert_respond_to(@bignum1, :+)
      assert_nothing_raised{ @bignum1 + @bignum2 }
      assert_equal(55340232221128654848, @bignum1 + @bignum2)
      assert_kind_of(Bignum, @bignum1 + @bignum2)
   end

   def test_subtraction
      assert_respond_to(@bignum1, :-)

      assert_nothing_raised{ @bignum2 - @bignum1 }
      assert_nothing_raised{ @bignum1 - @bignum2 }

      assert_equal(18446744073709551616, @bignum2 - @bignum1)
      assert_equal(-18446744073709551616, @bignum1 - @bignum2)

      assert_kind_of(Bignum, @bignum2 - @bignum1)
      assert_kind_of(Bignum, @bignum1 - @bignum2)
   end

   def test_multiplication
      assert_respond_to(@bignum1, :*)

      assert_nothing_raised{ @bignum1 * 2 }
      assert_nothing_raised{ @bignum1 * -2 }
      assert_nothing_raised{ @bignum1 * 0.5 }
      assert_nothing_raised{ @bignum1 * @bignum2 }

      assert_equal(36893488147419103232, @bignum1 * 2)
      assert_equal(0, @bignum1 * 0)

      assert_kind_of(Bignum, @bignum1 * @bignum2)
   end

   def test_division
      assert_respond_to(@bignum1, :/)

      assert_nothing_raised{ @bignum2 / @bignum1 }
      assert_nothing_raised{ @bignum1 / @bignum2 }
      assert_nothing_raised{ @bignum1 / 1 }
      assert_nothing_raised{ @bignum1 / -1 }
      assert_nothing_raised{ @bignum1 / 0.5 }

      assert_equal(@bignum1, @bignum1 / 1)
      assert_equal(3689348814741910323, @bignum1 / 5)

      assert_kind_of(Bignum, @bignum1 / 1)

      assert_raises(ZeroDivisionError){ @bignum1 / 0 }
   end

   def test_div_alias
      assert_respond_to(@bignum1, :div)

      assert_nothing_raised{ @bignum2.div(@bignum1) }
      assert_nothing_raised{ @bignum1.div(@bignum2) }
      assert_nothing_raised{ @bignum1.div(1) }
      assert_nothing_raised{ @bignum1.div(-1) }
      assert_nothing_raised{ @bignum1.div(0.5) }

      assert_equal(@bignum1, @bignum1.div(1))
      assert_equal(3689348814741910323, @bignum1.div(5))

      assert_kind_of(Bignum, @bignum1.div(1))

      assert_raises(ZeroDivisionError){ @bignum1.div(0) }
   end

   def test_modulo
      assert_respond_to(@bignum1, :%)

      assert_nothing_raised{ @bignum1 % @bignum2 }
      assert_nothing_raised{ @bignum2 % @bignum1 }
      assert_nothing_raised{ @bignum1 % @bignum1 }
      assert_nothing_raised{ @bignum1 % 1 }
      assert_nothing_raised{ @bignum1 % -1 }

      assert_equal(0, @bignum1 % @bignum1)
      assert_equal(0, @bignum2 % @bignum1)
      assert_equal(18446744073709551616, @bignum1 % @bignum2)

      assert_raises(ZeroDivisionError){ @bignum1 % 0 }
   end

   def test_modulo_alias
      assert_respond_to(@bignum1, :modulo)

      assert_nothing_raised{ @bignum1.modulo(@bignum2) }
      assert_nothing_raised{ @bignum2.modulo(@bignum1) }
      assert_nothing_raised{ @bignum1.modulo(@bignum1) }
      assert_nothing_raised{ @bignum1.modulo(1) }
      assert_nothing_raised{ @bignum1.modulo(-1) }

      assert_equal(0, @bignum1.modulo(@bignum1))
      assert_equal(0, @bignum2.modulo(@bignum1))
      assert_equal(18446744073709551616, @bignum1.modulo(@bignum2))

      assert_raises(ZeroDivisionError){ @bignum1.modulo(0) }
   end

   def test_exponentiation
      assert_respond_to(@bignum1, :**)

      assert_nothing_raised{ @bignum1 ** 0 }
      assert_nothing_raised{ @bignum1 ** 1 }
      assert_nothing_raised{ @bignum1 ** -1 }
      assert_nothing_raised{ @bignum1 ** 5 }
      assert_nothing_raised{ @bignum2 ** 5 }

      assert_equal(1, @bignum1 ** 0)
      assert_equal(@bignum1, @bignum1 ** 1)
   end

   def test_unary_minus
      assert_respond_to(@bignum1, :-@)
      assert_nothing_raised{ -@bignum1 }
      assert_equal(-18446744073709551616, -@bignum1)
      assert_equal(18446744073709551616, -(-@bignum1))
   end

   def teardown
      @bignum1 = nil
      @bignum2 = nil
   end
end
