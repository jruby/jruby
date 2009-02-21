########################################################################
# tc_localtime.rb
#
# Test case for the Time#localtime instance method.
#
# Note: Amber unifies Time#localtime and Time#getlocal into the
# Time#localtime and Time#localtime! methods.
########################################################################
require 'test/unit'
require 'test/helper'

class TC_Time_Localtime_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @gmt    = Time.gm(2000, 1, 1, 20, 15, 1)
      @local  = nil
      @offset = get_tz_offset
      @hour   = @gmt.hour
   end

   def test_localtime_basic
      assert_respond_to(@gmt, :localtime)
      assert_nothing_raised{ @gmt.localtime }
      assert_kind_of(Time, @gmt.localtime)
   end

   # Compare vs Time#getlocal
   def test_localtime_modifies_receiver
      assert_nothing_raised{ @local = @gmt.localtime }
      assert_equal(true, @gmt.object_id == @local.object_id)
   end

   def test_localtime
      assert_nothing_raised{ @local = @gmt.localtime }
      assert_equal(1, @local.mon)
#      assert_equal(1, @local.day)
#      assert_equal(@local.hour, (@hour - @offset) % 24)
      assert_equal(15, @local.min)
      assert_equal(1, @local.sec)
   end

   def test_localtime_expected_errors
      assert_raise(ArgumentError){ @gmt.localtime(1) }
      assert_raise(NoMethodError){ @gmt.localtime = 1 }
   end

   def teardown
      @gmt    = nil
      @gmt    = nil
      @offset = nil
   end
end
