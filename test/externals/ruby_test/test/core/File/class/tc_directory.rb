#####################################################################
# tc_directory.rb
#
# Test case for the File.directory? class method.
#####################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Directory_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      if WINDOWS
         @dir  = "C:\\"
         @file = "C:\\winnt\\notepad.exe"
      else
         @dir  = "/"
         @file = "/bin/ls"
      end
   end

   def test_directory_basic
      assert_respond_to(File, :directory?)
      assert_nothing_raised{ File.directory?(@dir) }
   end

   def test_directory
      assert_equal(true, File.directory?(@dir))
      assert_equal(false, File.directory?(@file))
   end

   def test_directory_expected_errors
      assert_raises(ArgumentError){ File.directory? }
      assert_raises(ArgumentError){ File.directory?(@dir, @file) }
      assert_raises(TypeError){ File.directory?(nil) }
   end

   def teardown
      @dir = nil
   end
end
