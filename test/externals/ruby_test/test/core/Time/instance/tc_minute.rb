###############################################################################
# tc_min.rb
#
# Test case for the Time#min instance method.
###############################################################################
require 'test/unit'

class TC_Time_Minute_InstanceMethod < Test::Unit::TestCase
   def setup
      @time = Time.mktime(2007, 6, 29, 7, 3, 27)
   end

   def test_min_basic
      assert_respond_to(@time, :min)
      assert_nothing_raised{ @time.min }
      assert_kind_of(Integer, @time.min)
   end

   def test_min
      assert_equal(3, @time.min)
      assert_equal(0, Time.mktime(2007).min)
      assert_equal(0, Time.mktime(2007, 2, 29).min)
   end

   def test_min_expected_errors
      assert_raise(ArgumentError){ @time.min(1) }
      assert_raise(NoMethodError){ @time.min = 1 }
   end

   def teardown
      @time = nil
   end
end
