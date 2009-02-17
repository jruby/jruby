#####################################################################
# tc_log10.rb
#
# Test cases for the Math.log10 method.
#####################################################################
require 'test/unit'
require 'test/helper'

class TC_Math_Log10_Class < Test::Unit::TestCase
   include Test::Helper
   
   def test_log10_basic
      assert_respond_to(Math, :log10)
      assert_nothing_raised{ Math.log10(1) }
      assert_nothing_raised{ Math.log10(100) }
      assert_kind_of(Float, Math.log10(1))
   end

   def test_log10_positive
      assert_nothing_raised{ Math.log10(1) }
      assert_in_delta(0.0, Math.log10(1), 0.01)
   end
   
   def test_log10_positive_float
      assert_nothing_raised{ Math.log10(0.345) }
      assert_in_delta(-0.46, Math.log10(0.345), 0.01)
   end
   
   if OSX || JRUBY
      def test_log10_returns_infinity
         assert_equal('-Infinity', Math.log10(0).to_s)
      end
   end
   
   # TODO: Shouldn't all non-numerics raise TypeError?
   def test_log10_expected_errors
      assert_raises(Errno::ERANGE){ Math.log10(0) } unless OSX || JRUBY
      assert_raises(Errno::EDOM){ Math.log10(-1) }
      assert_raises(TypeError){ Math.log10(nil) }
      assert_raises(ArgumentError){ Math.log10('test') }
   end
end
