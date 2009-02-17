#####################################################################
# tc_log.rb
#
# Test cases for the Math.log method.
#####################################################################
require 'test/unit'
require 'test/helper'

class TC_Math_Log_Class < Test::Unit::TestCase
   include Test::Helper

   def test_log_basic
      assert_respond_to(Math, :log)
      assert_nothing_raised{ Math.log(1) }
      assert_nothing_raised{ Math.log(100) }
      assert_kind_of(Float, Math.log(1))
   end

   def test_log_positive
      assert_nothing_raised{ Math.log(1) }
      assert_in_delta(0.0, Math.log(1), 0.01)
   end
   
   def test_log_positive_float
      assert_nothing_raised{ Math.log(0.345) }
      assert_in_delta(-1.06, Math.log(0.345), 0.01)
   end
   
   if OSX || JRUBY
      def test_log_returns_infinity
         assert_equal('-Infinity', Math.log(0).to_s)
      end
   end
   
   # TODO: Shouldn't all non-numerics raise TypeError?
   def test_log_expected_errors
      assert_raises(Errno::ERANGE){ Math.log(0) } unless OSX || JRUBY
      assert_raises(Errno::EDOM){ Math.log(-1) }
      assert_raises(TypeError){ Math.log(nil) }
      assert_raises(ArgumentError){ Math.log('test') }
   end
end
