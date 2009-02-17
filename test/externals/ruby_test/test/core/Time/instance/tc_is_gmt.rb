########################################################################
# tc_is_gmt.rb
#
# Test case for the Time#gmt? instance method and the Time#utc? alias.
########################################################################
require 'test/unit'
require 'test/helper'

class TC_Time_IsGmt_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @local = Time.local(2000, 1, 1, 20, 15, 1)
      @gmt   = Time.gm(2000, 1, 1, 20, 15, 1)
   end

   def test_is_gmt_basic
      assert_respond_to(@local, :gmt?)
      assert_nothing_raised{ @local.gmt? }
      assert_kind_of(Boolean, @local.gmt?)
   end

   def test_is_utc_alias_basic
      assert_respond_to(@local, :utc?)
      assert_nothing_raised{ @local.utc? }
      assert_kind_of(Boolean, @local.utc?)
   end
   
   def test_is_gmt
      assert_equal(true, @gmt.gmt?)
      assert_equal(false, @local.gmt?)
   end

   def test_is_utc_alias
      assert_equal(true, @gmt.utc?)
      assert_equal(false, @local.utc?)
   end

   def test_is_gmt_converted_from_local
      assert_equal(true, @gmt.getgm.utc?)
      assert_equal(true, @local.getgm.utc?)
   end

   def test_is_gmt_expected_errors
      assert_raise(ArgumentError){ @local.gmt?(1) }
   end

   def test_is_utc_alias_expected_errors
      assert_raise(ArgumentError){ @local.utc?(1) }
   end

   def teardown
      @local = nil
      @gmt   = nil
   end
end
