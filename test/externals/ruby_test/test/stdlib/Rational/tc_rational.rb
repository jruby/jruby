######################################################################
# tc_rational.rb
#
# Test case for the Rational class (and method).
######################################################################
require "rational"
require "test/unit"

class TC_Rational_Stdlib < Test::Unit::TestCase
   def setup
      @rat1 = Rational(3,4)      # Basic
      @rat2 = Rational(1,1)      # One
      @rat3 = Rational(2,3)      # Infinite division
      @rat4 = Rational(0,3)      # Zero numerator
      @rat5 = Rational(9)        # Default denominator
      @rat6 = Rational(3,-4)     # Negative denominator
      @rat7 = Rational(-3,-4)    # Both negative
      @rat8 = Rational(-3,4)     # Negative numerator
      @rat9 = Rational(0,-3)     # Zero numerator, negative denominator
   end

   def test_rational_abs
      assert_respond_to(@rat6, :abs)
   end

   def test_rational_multiplication
      assert_respond_to(@rat1, :*)

      assert_nothing_raised{ @rat1 * @rat2 }
      assert_nothing_raised{ @rat1 * @rat4 }
      assert_nothing_raised{ @rat1 * @rat7 }

      assert_equal(Rational(1,2), @rat1 * @rat3)
      assert_equal(Rational(9,16), @rat1 * @rat7)
      assert_equal(Rational(3,4), @rat1 * 1)
      assert_equal(Rational(9,4), @rat1 * 3)
      assert_equal(Rational(3,1), @rat1 * 4)
      assert_equal(0.375, @rat1 * 0.5)
   end

   def test_rational_divmod
      assert_respond_to(@rat1, :divmod)
      assert_nothing_raised{ @rat1.divmod(@rat2) }
      assert_equal([0, Rational(3,4)], @rat1.divmod(@rat2))
      assert_equal([1, Rational(1,12)], @rat1.divmod(@rat3))
      assert_equal([1, Rational(0,1)], @rat1.divmod(@rat7))    # ??
   end

   # We test .to_s since Rational has a custom implementation
   def test_rational_to_s
      assert_respond_to(@rat1, :to_s)
      assert_nothing_raised{ @rat1.to_s }
      assert_kind_of(String, @rat1.to_s)
      assert_equal("3/4", @rat1.to_s)
      assert_equal("3/4", @rat7.to_s)
   end

   # I don't entirely understand what Rational#coerce is supposed to do
   # or if it's even supposed to be public.
   def test_rational_coerce
      assert_respond_to(@rat1, :coerce)
      assert_nothing_raised{ @rat1.coerce(9) }
      assert_nothing_raised{ @rat1.coerce(0.5) }
      #assert_equal(Rational(9,1), @rat1.coerce(9))
   end

   # '%'
   def test_rational_modulo
      assert_respond_to(@rat1, :%)
      assert_nothing_raised{ @rat1 % @rat2 }
      assert_equal(Rational(3,4), @rat1 % @rat2)
      assert_equal(Rational(1,12), @rat1 % @rat3)
      assert_equal(Rational(0,1), @rat1 % @rat7)
      assert_equal(Rational(3,4), @rat1 % 2)
      assert_equal(Rational(-1,4), @rat1 % -1)
      assert_equal(0.25, @rat1 % 0.5)
   end

   # '/'
   def test_rational_division
      assert_respond_to(@rat1, :/)
      assert_nothing_raised{ @rat1 / @rat2 }
      assert_equal(Rational(3,4), @rat1 / @rat2)
      assert_equal(Rational(3,2), @rat1 / Rational(1,2))
      assert_equal(Rational(3,8), @rat1 / 2)
      assert_equal(0.375, @rat1 / 2.0)
   end

   def test_rational_hash
      assert_respond_to(@rat1, :hash)
      assert_nothing_raised{ @rat1.hash }
      assert_nothing_raised{ @rat4.hash }
      assert_kind_of(Fixnum, @rat1.hash)
   end

   # '=='
   def test_rational_equality
      assert_respond_to(@rat1, :==)

      assert(@rat1 == @rat1)
      assert(@rat1 == Rational(9,12))
      assert(@rat4 == @rat9)
      assert(@rat6 == @rat8)
      assert(@rat1 == 0.75)
      assert(@rat6 == @rat8)

      assert_equal(false, @rat1 == @rat2)
   end

   # '+'
   def test_rational_add
      assert_respond_to(@rat1, :+)
      assert_nothing_raised{ @rat1 + @rat2 }
      assert_nothing_raised{ @rat1 + @rat4 }
      assert_equal(Rational(17, 12), @rat1 + @rat3)
      assert_equal(Rational(3,4), @rat1 + @rat4)
      assert_equal(Rational(3,2), @rat1 + @rat7)
      assert_equal(Rational(0,1), @rat1 + @rat8)
   end

   def test_custom_constructor
      assert_respond_to(Rational, :new!)
      assert_nothing_raised{ Rational.new!(1,2) }
      assert_kind_of(Rational, Rational.new!(1,2))
   end

   def test_rational_basic_
      assert_kind_of(Rational, @rat1)
      assert_nothing_raised{ Rational(0,1) }
      assert_nothing_raised{ Rational(Rational(1,2), 1) }
   end

   def test_rational_numerator
      assert_respond_to(@rat1, :numerator)
      assert_equal(3, @rat1.numerator)
      assert_equal(0, @rat4.numerator)
      assert_equal(9, @rat5.numerator)
      assert_equal(-3, @rat6.numerator)
      assert_equal(3, @rat7.numerator)
      assert_equal(-3, @rat8.numerator)
      assert_equal(0, @rat9.numerator)
   end

   def test_rational_denominator
      assert_respond_to(@rat1, :denominator)
      assert_equal(4, @rat1.denominator)
      assert_equal(1, @rat4.denominator) # Reduced
      assert_equal(1, @rat5.denominator)
      assert_equal(4, @rat6.denominator)
      assert_equal(4, @rat7.denominator)
      assert_equal(4, @rat8.denominator)
      assert_equal(1, @rat9.denominator) # Reduced
   end

   def test_rational_to_f
      assert_respond_to(@rat1, :to_f)
      assert_equal(0.75, @rat1.to_f)
      assert_equal(1.0, @rat2.to_f)
      assert_equal(0.0, @rat4.to_f)
      assert_equal(-0.75, @rat6.to_f)
      assert_equal(0.75, @rat7.to_f)
      assert_equal(-0.75, @rat8.to_f)
      assert_equal(0.0, @rat9.to_f)
   end

   def test_rational_to_i
      assert_respond_to(@rat2, :to_i)
      assert_equal(0, @rat1.to_i)
      assert_equal(1, @rat2.to_i)
      assert_equal(0, @rat4.to_i)
      assert_equal(9, @rat5.to_i)
      # JRUBY-3860 (resolved -- we match 1.8.7 and 1.9.2 behavior now)
      #assert_equal(-1, @rat6.to_i)
      assert_equal(0, @rat7.to_i)
      # JRUBY-3860 (resolved -- we match 1.8.7 and 1.9.2 behavior now)
      #assert_equal(-1, @rat8.to_i)
      assert_equal(0, @rat9.to_i)
   end

   # '**'
   def test_rational_exponentiation
      assert_respond_to(@rat1, :**)
   end

   # '-'
   def test_rational_subtract
      assert_respond_to(@rat1, :-)
      assert_nothing_raised{ @rat1 - @rat2 }
      assert_nothing_raised{ @rat1 - @rat4 }
      assert_equal(Rational(1, 12), @rat1 - @rat3)
      assert_equal(Rational(3,4), @rat1 - @rat4)
   end

   # '<=>'
   def test_rational_compare
      assert_respond_to(@rat1, :<=>)
      assert_nothing_raised{ @rat1 <=> @rat2 }
      assert_nothing_raised{ @rat1 <=> @rat4 }
      assert_equal(-1, @rat1 <=> @rat2)
      assert_equal(1, @rat1 <=> @rat3)
      assert_equal(0, @rat1 <=> Rational(3,4))
   end

   # We test this since Rational has a custom inspect method
   def test_rational_inspect
      assert_respond_to(@rat1, :inspect)
      assert_equal("Rational(3, 4)", @rat1.inspect)
      assert_equal("Rational(0, 1)", @rat4.inspect) # Reduced
      assert_equal("Rational(9, 1)", @rat5.inspect)
   end

   def test_rational_expected_errors
      assert_raises(NoMethodError){ Rational.new(3,4) }

      assert_raises(ArgumentError){ Rational() }
      assert_raises(ArgumentError){ Rational(1,2,3) }
      assert_raises(ArgumentError){ Rational(3,4).coerce("foo") }

      assert_raises(ZeroDivisionError){ Rational(0,0) }
      assert_raises(ZeroDivisionError){ Rational(1,0) }
      assert_raises(ZeroDivisionError){ Rational(0,3) / Rational(0,8) }
      assert_raises(ZeroDivisionError){ Rational(3,4) % Rational(0,3) }
      assert_raises(ZeroDivisionError){ Rational(3,4).divmod Rational(0,3) } 

      # These should raise TypeError but currently raise NoMethodError (gcd)
      assert_raises(NoMethodError){ Rational("bogus", 1) }
      assert_raises(NoMethodError){ Rational(1.5, 2.5) }
      assert_raises(NoMethodError){ Rational(nil) }
      assert_raises(NoMethodError){ Rational(true) }
      assert_raises(NoMethodError){ Rational(false) }
#      assert_raises(NoMethodError){ Rational(1, Rational(2,3)) }
   end

   def teardown
      @rat1 = nil
      @rat2 = nil
      @rat3 = nil
      @rat4 = nil
      @rat5 = nil
   end
end
