###############################################################################
# tc_day.rb
#
# Test case for the Time#day instance method and the Time#mday alias.
###############################################################################
require 'test/unit'

class TC_Time_Day_InstanceMethod < Test::Unit::TestCase
   def setup
      @time = Time.mktime(2007, 6, 29)
   end

   def test_day_basic
      assert_respond_to(@time, :day)
      assert_nothing_raised{ @time.day }
      assert_kind_of(Integer, @time.day)
   end

   def test_mday_basic
      assert_respond_to(@time, :mday)
      assert_nothing_raised{ @time.mday }
      assert_kind_of(Integer, @time.mday)
   end

   def test_day
      assert_equal(29, @time.day)
      assert_equal(1, Time.mktime(2007).day)
   end

   def test_day_leap_year
      assert_equal(29, Time.mktime(2004, 2, 29).day)
      assert_equal(1, Time.mktime(2007, 2, 29).day)
   end

   def test_mday
      assert_equal(29, @time.mday)
      assert_equal(1, Time.mktime(2007).mday)
      assert_equal(1, Time.mktime(2007, 2, 29).mday)
      assert_equal(true, @time.mday == @time.day)
   end

   def test_mday_leap_year
      assert_equal(29, Time.mktime(2004, 2, 29).mday)
      assert_equal(1, Time.mktime(2007, 2, 29).mday)
   end

   def test_day_expected_errors
      assert_raise(ArgumentError){ @time.day(1) }
      assert_raise(NoMethodError){ @time.day = 1 }
      assert_raise(ArgumentError){ @time.mday(1) }
      assert_raise(NoMethodError){ @time.mday = 1 }
   end

   def teardown
      @time = nil
   end
end
