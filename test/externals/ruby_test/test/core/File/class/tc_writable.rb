#####################################################################
# tc_writable.rb
#
# Test case for the File.writable? class method. Some tests
# skipped on MS Windows.
#####################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Writable_ClassMethod < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      @file1 = File.join(Dir.pwd, 'temp1.txt')
      @file2 = File.join(Dir.pwd, 'temp2.txt')

      touch(@file1)
      touch(@file2)
      
      File.chmod(0766, @file1)
      File.chmod(0444, @file2)
   end

   def test_writable_basic
      assert_respond_to(File, :writable?)
      assert_nothing_raised{ File.writable?(@file1) }
   end

   unless WINDOWS
      def test_writable
         assert_equal(true, File.writable?(@file1))
         assert_equal(false, File.writable?(@file2))
         assert_equal(false, File.writable?('bogus'))
      end
   end

   def test_writable_expected_errors
      assert_raises(ArgumentError){ File.writable? }
      assert_raises(TypeError){ File.writable?(1) }
      assert_raises(TypeError){ File.writable?(nil) }
      assert_raises(TypeError){ File.writable?(false) }
   end

   def teardown
      remove_file(@file1)
      remove_file(@file2)

      @file1 = nil
      @file2 = nil
   end
end
