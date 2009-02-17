###############################################################################
# tc_hour.rb
#
# Test case for the Time#hour instance method.
###############################################################################
require 'test/unit'

class TC_Time_Hour_InstanceMethod < Test::Unit::TestCase
   def setup
      @time = Time.mktime(2007, 6, 29, 7, 3, 27)
   end

   def test_hour_basic
      assert_respond_to(@time, :hour)
      assert_nothing_raised{ @time.hour }
      assert_kind_of(Integer, @time.hour)
   end

   def test_hour
      assert_equal(7, @time.hour)
      assert_equal(0, Time.mktime(2007).hour)
      assert_equal(0, Time.mktime(2007, 2, 29).hour)
   end

   def test_hour_expected_errors
      assert_raise(ArgumentError){ @time.hour(1) }
      assert_raise(NoMethodError){ @time.hour = 1 }
   end

   def teardown
      @time = nil
   end
end
