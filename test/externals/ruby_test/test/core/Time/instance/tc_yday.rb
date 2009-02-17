###############################################################################
# tc_yday.rb
#
# Test case for the Time#yday instance method.
###############################################################################
require 'test/unit'

class TC_Time_Yday_InstanceMethod < Test::Unit::TestCase
   def setup
      @time = Time.mktime(2007, 6, 29)
   end

   def test_yday_basic
      assert_respond_to(@time, :yday)
      assert_nothing_raised{ @time.yday }
      assert_kind_of(Integer, @time.yday)
   end

   def test_yday
      assert_equal(180, @time.yday)
      assert_equal(1, Time.mktime(2007).yday)
      assert_equal(60, Time.mktime(2007, 2, 29).yday)
      assert_equal(365, Time.mktime(2007, 12, 31).yday)
   end

   def test_yday_leap_year
      assert_equal(60, Time.mktime(2004, 2, 29).yday)
      assert_equal(366, Time.mktime(2004, 12, 31).yday)
   end

   def test_yday_expected_errors
      assert_raise(ArgumentError){ @time.yday(1) }
      assert_raise(NoMethodError){ @time.yday = 1 }
   end

   def teardown
      @time = nil
   end
end
