###############################################################################
# tc_at.rb
###############################################################################
require 'test/unit'
require 'test/helper'

class TC_Time_At_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @seconds = 981173106 # Feb 2, 2001, 21:05:06, -0700
      @time    = nil
   end

   def test_at_basic
      assert_respond_to(Time, :at)
      assert_nothing_raised{ Time.at(0) }
      assert_kind_of(Time, Time.at(0))
   end

   def test_at
      assert_equal(0, Time.at(0).to_i)
      assert_equal(@seconds, Time.at(@seconds).to_i)
   end

   def test_at_with_microseconds
      assert_equal(@seconds, Time.at(@seconds, 885).tv_sec)
      assert_equal(885, Time.at(@seconds, 885).tv_usec)
   end

   def test_at_with_float
      assert_equal(@seconds, Time.at(981173106.7).to_i)
      assert_equal(@seconds, Time.at(981173106.7, 300).to_i)
      assert_equal(@seconds, Time.at(981173106.7, 300.5).to_i)
   end

   def test_at_with_time_argument
      assert_nothing_raised{ @time = Time.at(@seconds, 800) }
      assert_equal(@seconds, Time.at(@time).tv_sec)
      assert_equal(800, Time.at(@time).tv_usec)
   end

   unless WINDOWS
      def test_at_with_negative_values
         assert_nothing_raised{ Time.at(-100) } 
         assert_nothing_raised{ Time.at(-100, -200) } 
         assert_equal(-100, Time.at(-100).tv_sec)
      end
   end

   def test_at_expected_errors
      assert_raise(ArgumentError){ Time.at }
      assert_raise(ArgumentError){ Time.at(1, 2, 3) }
   end

   def teardown
      @seconds = nil
      @time    = nil
   end
end
