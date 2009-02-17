######################################################################
# tc_sticky.rb
#
# Test case for the FileStat#sticky instance method. Most tests
# are skipped on MS Windows.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_FileStat_Sticky_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @stat = File::Stat.new(__FILE__)
   end

   def test_sticky_basic
      assert_respond_to(@stat, :sticky?)
   end

   def test_sticky
      assert_equal(false, @stat.sticky?)
      assert_equal(true, File::Stat.new('/tmp').sticky?) unless WINDOWS
   end

   def test_sticky_expected_errors
      assert_raises(ArgumentError){ @stat.sticky?(1) }
   end

   def teardown
      @stat = nil
      @file = nil
   end
end
