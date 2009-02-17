######################################################################
# tc_blockdev.rb
#
# Test case for the FileStat#blockdev instance method.
#
# TODO: Use WMI + WIN32_LOGICALDISK to get the CDROM drive. Even if
# we use that technique, however, the test will fail unless there's a
# disk in the drive.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_FileStat_Blockdev_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @stat = File::Stat.new(__FILE__)

      if SOLARIS
         @file = '/dev/fd0'
      elsif WINDOWS
         @file = 'D:/'
      else
         @file = '/dev/disk0'
      end
      
      @win_error = '=> May fail on MS Windows'
   end

   def test_blockdev_basic
      assert_respond_to(@stat, :blockdev?)
   end

   def test_blockdev
      assert_equal(false, @stat.blockdev?)
      assert_equal(true, File.stat(@file).blockdev?, @win_error)
   end

   def test_blockdev_expected_errors
      assert_raises(ArgumentError){ @stat.blockdev?(1) }
   end

   def teardown
      @stat = nil
      @file = nil
      @win_error = nil
   end
end
