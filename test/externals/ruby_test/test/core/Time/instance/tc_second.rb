###############################################################################
# tc_sec.rb
#
# Test case for the Time#sec instance method.
###############################################################################
require 'test/unit'

class TC_Time_Second_InstanceMethod < Test::Unit::TestCase
   def setup
      @time = Time.mktime(2007, 6, 29, 7, 22, 39)
   end

   def test_sec_basic
      assert_respond_to(@time, :sec)
      assert_nothing_raised{ @time.sec }
      assert_kind_of(Integer, @time.sec)
   end

   def test_sec
      assert_equal(39, @time.sec)
      assert_equal(0, Time.mktime(2007).sec)
      assert_equal(0, Time.mktime(2004, 2, 29).sec)
      assert_equal(0, Time.mktime(2007, 2, 29).sec)
      assert_equal(0, Time.mktime(2007, 2, 29, 1).sec)
      assert_equal(0, Time.mktime(2007, 2, 29, 1, 2).sec)
   end

   def test_sec_expected_errors
      assert_raise(ArgumentError){ @time.sec(1) }
      assert_raise(NoMethodError){ @time.sec = 1 }
   end

   def teardown
      @time = nil
   end
end
