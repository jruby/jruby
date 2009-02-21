########################################################################
# tc_getlocal.rb
#
# Test case for the Time#getlocal instance method.
########################################################################
require 'test/unit'
require 'test/helper'

class TC_Time_Getlocal_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @gmt    = Time.gm(2000, 1, 1, 20, 15, 1)
      @local  = nil
      @offset = get_tz_offset
      @local_day = if (@gmt.hour + @offset) >= 24
         @gmt.day + 1
      elsif (@gmt.hour + @offset) < 0
         @gmt.day - 1
      else
         @gmt.day
      end

   end

   def test_getlocal_basic
      assert_respond_to(@gmt, :getlocal)
      assert_nothing_raised{ @gmt.getlocal }
      assert_kind_of(Time, @gmt.getlocal)
   end

   # Compare vs Time#localtime
   def test_getlocal_receiver_not_mofified
      assert_nothing_raised{ @local = @gmt.getlocal }
      assert_equal(true, @local.object_id != @gmt.object_id)
   end

   def test_getlocal
      assert_nothing_raised{ @local = @gmt.getlocal }
      assert_equal(1, @local.mon)
#      assert_equal(@local_day, @local.day)
#      assert_equal(@local.hour, (@gmt.hour - @offset) % 24)
      assert_equal(15, @local.min)
      assert_equal(1, @local.sec)
   end

   def test_getlocal_expected_errors
      assert_raise(ArgumentError){ @gmt.getlocal(1) }
      assert_raise(NoMethodError){ @gmt.getlocal = 1 }
   end

   def teardown
      @gmt    = nil
      @gmt    = nil
      @offset = nil
   end
end
