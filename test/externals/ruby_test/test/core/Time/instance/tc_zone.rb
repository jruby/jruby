########################################################################
# tc_zone.rb
#
# Test case for the Time#zone instance method.
########################################################################
require 'test/unit'
require 'test/helper'

class TC_Time_Zone_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @gmt   = Time.gm(2000, 6, 1, 20, 15, 1)
      @local = Time.local(2000, 6, 1, 20, 15, 1)
      @tz    = get_tz_name
   end

   def test_zone_basic
      assert_respond_to(@local, :zone)
      assert_nothing_raised{ @local.zone }
   end

   def test_zone
      assert_equal('UTC', @gmt.zone)
#      assert_equal(@tz, @local.zone)
   end

   def test_zone_expected_errors
      assert_raise(ArgumentError){ @local.zone(1) }
      assert_raise(NoMethodError){ @local.zone = 'UTC' }
   end

   def teardown
      @gmt   = nil
      @local = nil
      @tz    = nil
   end
end
