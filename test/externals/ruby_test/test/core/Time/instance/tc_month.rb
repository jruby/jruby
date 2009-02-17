###############################################################################
# tc_month.rb
#
# Test case for the Time#month instance method and the Time#mon alias.
###############################################################################
require 'test/unit'

class TC_Time_Month_InstanceMethod < Test::Unit::TestCase
   def setup
      @time = Time.mktime(2007, 6, 29)
   end

   def test_month_basic
      assert_respond_to(@time, :month)
      assert_nothing_raised{ @time.month }
      assert_kind_of(Integer, @time.month)
   end

   def test_mon_basic
      assert_respond_to(@time, :mon)
      assert_nothing_raised{ @time.mon }
      assert_kind_of(Integer, @time.mon)
   end

   def test_month
      assert_equal(6, @time.month)
      assert_equal(1, Time.mktime(2007).month)
      assert_equal(2, Time.mktime(2004, 2, 29).month)
      assert_equal(3, Time.mktime(2007, 2, 29).month)
   end

   def test_mon
      assert_equal(6, @time.mon)
      assert_equal(1, Time.mktime(2007).mon)
      assert_equal(2, Time.mktime(2004, 2, 29).mon)
      assert_equal(3, Time.mktime(2007, 2, 29).mon)
      assert_equal(true, @time.month == @time.mon)
   end

   def test_month_expected_errors
      assert_raise(ArgumentError){ @time.month(1) }
      assert_raise(NoMethodError){ @time.month = 1 }
      assert_raise(ArgumentError){ @time.mon(1) }
      assert_raise(NoMethodError){ @time.mon = 1 }
   end

   def teardown
      @time = nil
   end
end
