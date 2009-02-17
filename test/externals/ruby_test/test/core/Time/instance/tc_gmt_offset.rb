########################################################################
# tc_gmt_offset.rb
#
# Test case for the Time#gmt_offset instance method and the
# Time#utc_offset alias.
########################################################################
require 'test/unit'
require 'test/helper'

class TC_Time_GmtOffset_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @local  = Time.local(2000, 1, 1, 20, 15, 1)
      @gmt    = Time.gm(2000, 1, 1, 20, 15, 1)
      @offset = get_tz_offset * 60 * 60
   end

   def test_gmt_offset_basic
      assert_respond_to(@local, :gmt_offset)
      assert_nothing_raised{ @local.gmt_offset }
      assert_kind_of(Fixnum, @local.gmt_offset)
   end

   def test_gmt_offset
      assert_equal(0, @gmt.gmt_offset)
#      assert_equal(-@offset, @local.gmt_offset)
   end

   def test_utc_offset_alias
      assert_equal(0, @gmt.gmt_offset)
#      assert_equal(-@offset, @local.gmt_offset)
   end

   def test_gmt_offset_expected_errors
      assert_raise(ArgumentError){ @local.gmt_offset(1) }
   end

   def test_utc_offset_alias_expected_errors
      assert_raise(ArgumentError){ @local.utc_offset(1) }
      assert_raise(NoMethodError){ @local.utc_offset = 1 }
   end

   def teardown
      @local  = nil
      @gmt    = nil
      @offset = nil
   end
end
