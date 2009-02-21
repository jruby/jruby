########################################################################
# tc_gmtime.rb
#
# Test case for the Time#gmtime instance method and the Time#utc
# alias.
#
# NOTE: Amber unified the Time#getgm and Time#gmtime methods into the
# Time#gmtime and Time#gmtime! methods.
########################################################################
require 'test/unit'
require 'test/helper'

class TC_Time_Gmtime_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @local  = Time.local(2000, 1, 1, 20, 15, 1)
      @hour   = @local.hour
      @gmt    = nil
      @offset = get_tz_offset
   end

   def test_gmtime_basic
      assert_respond_to(@local, :gmtime)
      assert_nothing_raised{ @local.gmtime }
      assert_kind_of(Time, @local.gmtime)
   end

   def test_utc_alias_basic
      assert_respond_to(@local, :utc)
      assert_nothing_raised{ @local.utc }
      assert_kind_of(Time, @local.utc)
   end

   # Compare vs Time#getgm
   def test_gmtime_receiver_modified
      assert_nothing_raised{ @gmt = @local.gmtime }
      assert_equal(true, @local.gmt?)
      assert_equal(true, @gmt.object_id == @local.object_id)
   end

   def test_utc_alias_receiver_modified
      assert_nothing_raised{ @gmt = @local.utc }
      assert_equal(true, @local.gmt?)
      assert_equal(true, @gmt.object_id == @local.object_id)
   end

   def test_gmtime
      assert_nothing_raised{ @local.gmtime }
      assert_equal(1, @local.mon)
#      assert_equal(@local.hour, (@hour + @offset) % 24)
#      assert_equal(2, @local.day)
      assert_equal(15, @local.min)
      assert_equal(1, @local.sec)
   end

   def test_utc_alias
      assert_nothing_raised{ @local.utc }
      assert_equal(1, @local.mon)
#      assert_equal(@local.hour, (@hour + @offset) % 24)
#      assert_equal(2, @local.day)
      assert_equal(15, @local.min)
      assert_equal(1, @local.sec)
   end

   def test_gmtime_expected_errors
      assert_raise(ArgumentError){ @local.gmtime(1) }
   end

   def test_utc_alias_expected_errors
      assert_raise(ArgumentError){ @local.utc(1) }
      assert_raise(NoMethodError){ @local.utc = 1 }
   end

   def teardown
      @local  = nil
      @hour   = nil
      @gmt    = nil
      @offset = nil
   end
end
