#####################################################################
# tc_file.rb
#
# Test case for the File.file? class method.
#####################################################################
require 'test/unit'
require 'test/helper'

class TC_File_File_ClassMethod < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      if WINDOWS
         @null = "NUL"
         @dir  = "C:\\"
      else
         @null = "/dev/null"
         @dir  = "/bin"
      end

      @msg  = '=> May fail on MS Windows'
      @file = "test.txt"
      touch(@file)
   end

   def test_file_basic
      assert_respond_to(File, :file?)
      assert_nothing_raised{ File.file?(@file) }
   end

   def test_file
      assert_equal(true, File.file?(@file))
      assert_equal(false, File.file?(@dir))
      assert_equal(false, File.file?(@null), @msg)
   end

   def test_file_expected_errors
      assert_raises(ArgumentError){ File.file? }
      assert_raises(ArgumentError){ File.file?(@null, @file) }
      assert_raises(TypeError){ File.file?(nil) }
   end

   def teardown
      remove_file(@file)
      @null = nil
      @file = nil
      @msg  = nil
   end
end
