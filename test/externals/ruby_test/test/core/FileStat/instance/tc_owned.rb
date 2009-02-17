######################################################################
# tc_owned.rb
#
# Test case for the FileStat#owned? instance method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_FileStat_Owned_InstanceMethod < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      @stat = File::Stat.new(__FILE__)
   end

   def test_owned_basic
      assert_respond_to(@stat, :owned?)
   end

   # Windows always returns true
   def test_owned
      assert_equal(true, @stat.owned?)
      assert_equal(false, File::Stat.new('/').owned?) unless WINDOWS
   end

   def test_owned_expected_errors
      assert_raises(ArgumentError){ @stat.owned?(1) }
   end

   def teardown
      @stat = nil
   end
end
