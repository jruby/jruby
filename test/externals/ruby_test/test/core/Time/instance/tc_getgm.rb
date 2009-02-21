########################################################################
# tc_getgm.rb
#
# Test case for the Time#getgm instance method and the Time#getutc
# alias.
########################################################################
require 'test/unit'
require 'test/helper'

class TC_Time_Getgm_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @local  = Time.local(2000, 1, 1, 20, 15, 1)
      @gmt    = nil
      @offset = get_tz_offset
      @gmt_day = if @offset > @local.hour
         @local.day - 1
      elsif (@local.hour - @offset) >= 24
         @local.day + 1
      else
         @local.day
      end
   end

   def test_getgm_basic
      assert_respond_to(@local, :getgm)
      assert_nothing_raised{ @local.getgm }
      assert_kind_of(Time, @local.getgm)
   end

   def test_utc_alias_basic
      assert_respond_to(@local, :getutc)
      assert_nothing_raised{ @local.getutc }
      assert_kind_of(Time, @local.getutc)
   end

   def test_getgm
      assert_nothing_raised{ @gmt = @local.getgm }
      assert_equal(true, @gmt.gmt?)
      assert_equal(1, @gmt.mon)
#      assert_equal(@gmt.hour, (@local.hour - @offset) % 24)
#      assert_equal(@gmt_day, @gmt.day)
      assert_equal(15, @gmt.min)
      assert_equal(1, @gmt.sec)
   end

   def test_getutc_alias
      assert_nothing_raised{ @gmt = @local.getutc }
      assert_equal(true, @gmt.gmt?)
      assert_equal(1, @gmt.mon)
#      assert_equal(@gmt.hour, (@local.hour - @offset) % 24)
#      assert_equal(@gmt_day, @gmt.day)
      assert_equal(15, @gmt.min)
      assert_equal(1, @gmt.sec)
   end

   # Compare vs Time#gmtime
   def test_getgm_receiver_unmodified
      assert_nothing_raised{ @gmt = @local.getgm }
      assert_equal(true, @gmt.object_id != @local.object_id)
   end

   def test_getutc_alias_receiver_unmodified
      assert_nothing_raised{ @gmt = @local.getutc }
      assert_equal(true, @gmt.object_id != @local.object_id)
   end

   def test_getgm_expected_errors
      assert_raise(ArgumentError){ @local.getgm(1) }
      assert_raise(NoMethodError){ @local.getgm = 1 }
   end

   def test_getutc_alias_expected_errors
      assert_raise(ArgumentError){ @local.getutc(1) }
      assert_raise(NoMethodError){ @local.getutc = 1 }
   end

   def teardown
      @local  = nil
      @gmt    = nil
      @offset = nil
   end
end
