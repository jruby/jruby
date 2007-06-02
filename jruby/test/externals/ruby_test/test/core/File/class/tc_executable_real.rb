#####################################################################
# tc_executable_real.rb
#
# Test case for the File.executable_real? class method. Some tests
# skipped on MS Windows.
#####################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Executable_Real_ClassMethod < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      @file1 = File.join(Dir.pwd, 'temp1.txt')
      @file2 = File.join(Dir.pwd, 'temp2.txt')

      touch(@file1)
      touch(@file2)
      
      File.chmod(0755, @file1)
   end

   def test_executable_real_basic
      assert_respond_to(File, :executable_real?)
      assert_nothing_raised{ File.executable_real?(@file1) }
   end

   unless WINDOWS
      def test_executable_real
         assert_equal(true, File.executable_real?(@file1))
         assert_equal(false, File.executable_real?(@file2))
         assert_equal(false, File.executable_real?('bogus'))
      end
   end

   def test_executable_real_expected_errors
      assert_raises(ArgumentError){ File.executable_real? }
      assert_raises(TypeError){ File.executable_real?(1) }
      assert_raises(TypeError){ File.executable_real?(nil) }
      assert_raises(TypeError){ File.executable_real?(false) }
   end

   def teardown
      remove_file(@file1)
      remove_file(@file2)

      @file1 = nil
      @file2 = nil
   end
end
