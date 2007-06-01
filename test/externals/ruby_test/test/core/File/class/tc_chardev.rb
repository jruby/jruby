#####################################################################
# tc_chardev.rb
#
# Test case for the File.chardev? class method.
#####################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Chardev_ClassMethod < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      if WINDOWS
         @char_dev  = 'NUL'
         @other_dev = "C:\\"
      else
         @char_dev  = "/dev/null"
         @other_dev = "/"
      end
   end

   def test_chardev_basic
      assert_respond_to(File, :chardev?)
      assert_nothing_raised{ File.chardev?(@char_dev) }
   end

   # As of Ruby 1.8.6, the File.chardev? method is not supported on Windows,
   # although it is possible to implement. See win32-file, for example.
   # 
   def test_chardev
      if WINDOWS
         assert_equal(false, File.chardev?(@char_dev))
      else
         assert_equal(true, File.chardev?(@char_dev))
      end
      assert_equal(false, File.chardev?(@other_dev))
      assert_equal(false, File.chardev?('bogus')) # Errno::ENOENT?
   end

   def test_chardev_expected_errors
      assert_raises(ArgumentError){ File.chardev? }
      assert_raises(ArgumentError){ File.chardev?(@char_dev, @other_dev) }
      assert_raises(TypeError){ File.chardev?(nil) }
   end

   def teardown
      @char_dev  = nil
      @other_dev = nil
   end
end
