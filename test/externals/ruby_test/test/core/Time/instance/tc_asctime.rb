###############################################################################
# tc_asctime.rb
#
# Test case for the Time#asctime instance method, and the Time#ctime alias.
###############################################################################
require 'test/unit'

class TC_Time_Asctime_InstanceMethod < Test::Unit::TestCase
   def setup
      @time = Time.mktime(2007, 6, 29, 1, 2, 32)
   end

   def test_asctime_basic
      assert_respond_to(@time, :asctime)
      assert_nothing_raised{ @time.asctime }
      assert_kind_of(String, @time.asctime)
   end

   def test_ctime_basic
      assert_respond_to(@time, :ctime)
      assert_nothing_raised{ @time.ctime }
      assert_kind_of(String, @time.ctime)
   end

   def test_asctime
      assert_equal('Fri Jun 29 01:02:32 2007', @time.asctime)
   end

   def test_ctime
      assert_equal('Fri Jun 29 01:02:32 2007', @time.ctime)
      assert_equal(@time.ctime, @time.asctime)
   end

   def test_asctime_expected_errors
      assert_raise(ArgumentError){ @time.asctime(1) }
      assert_raise(ArgumentError){ @time.ctime(1) }
      assert_raise(NoMethodError){ @time.asctime = 1 }
   end

   def teardown
      @time = nil
   end
end
