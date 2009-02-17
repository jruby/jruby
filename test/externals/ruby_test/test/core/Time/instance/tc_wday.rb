###############################################################################
# tc_wday.rb
#
# Test case for the Time#wday instance method.
###############################################################################
require 'test/unit'

class TC_Time_Wday_InstanceMethod < Test::Unit::TestCase
   def setup
      @time = Time.mktime(2007, 6, 29)
   end

   def test_wday_basic
      assert_respond_to(@time, :wday)
      assert_nothing_raised{ @time.wday }
      assert_kind_of(Integer, @time.wday)
   end

   def test_wday
      assert_equal(5, @time.wday)
      assert_equal(1, Time.mktime(2007).wday)
      assert_equal(4, Time.mktime(2007, 2, 29).wday)
      assert_equal(1, Time.mktime(2007, 12, 31).wday)
   end

   def test_wday_leap_year
      assert_equal(0, Time.mktime(2004, 2, 29).wday)
      assert_equal(5, Time.mktime(2004, 12, 31).wday)
   end

   def test_wday_expected_errors
      assert_raise(ArgumentError){ @time.wday(1) }
      assert_raise(NoMethodError){ @time.wday = 1 }
   end

   def teardown
      @time = nil
   end
end
