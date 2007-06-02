#####################################################################
# tc_blockdev.rb
#
# Test case for the File.blockdev? class method.
#
# For this test case I do my best to find a block device (floppy, 
# etc) in a cross platform way, but there is no guarantee.  Thus,
# some tests will fail if a block device can't be found.
#
# Also, the definition of a block device is looser on Windows, where
# anything that isn't a character, pipe or disk file type is
# considered a block device.
#####################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Block_ClassMethod < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      if WINDOWS
         @block_dev = "NUL"
         @other_dev = "C:\\"
      else
         if File.exists?("/dev/fd0")
            @block_dev = "/dev/fd0"
         elsif File.exists?("/dev/diskette")
            @block_dev = "/dev/diskette"
         elsif File.exists?("/dev/cdrom")
            @block_dev = "/dev/cdrom"
         elsif File.exists?("/dev/sr0") # CDROM
            @block_dev = "/dev/sr0"
         elsif File.exists?("/dev/disk0")
            @block_dev = "/dev/disk0"
         else
            @block_dev = nil
         end
         @other_dev = "/usr/bin"
      end
   end

   def test_blockdev_basic
      assert_respond_to(File, :blockdev?)
      assert_nothing_raised{ File.blockdev?(@block_dev) }
   end

   def test_blockdev
      if WINDOWS
         assert_equal(false, File.blockdev?(@block_dev))
      else
         assert_equal(true, File.blockdev?(@block_dev))
      end
      assert_equal(false, File.blockdev?(@other_dev))
      assert_equal(false, File.blockdev?('bogus')) # Not Errno::ENOENT ???
   end

   def test_blockdev_expected_errors
      assert_raises(ArgumentError){ File.blockdev? }
      assert_raises(ArgumentError){ File.blockdev?(@block_dev, @other_dev) }
      assert_raises(TypeError){ File.blockdev?(nil) }
   end

   def teardown
      @block_dev = nil
   end
end
