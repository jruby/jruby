######################################################################
# tc_symlink.rb
#
# Test case for the FileStat#symlink? instance method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_FileStat_Symlink_InstanceMethod < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      @stat = File::Stat.new(__FILE__)
   end

   def test_symlink_basic
      assert_respond_to(@stat, :symlink?)
   end

   def test_symlink
      assert_equal(false, @stat.symlink?)
      assert_equal(false, File::Stat.new('/dev/stdin').symlink?) unless WINDOWS
   end

   def test_symlink_expected_errors
      assert_raises(ArgumentError){ @stat.symlink?(1) }
   end

   def teardown
      @stat = nil
   end
end
