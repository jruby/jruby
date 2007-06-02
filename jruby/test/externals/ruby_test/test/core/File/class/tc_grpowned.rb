#####################################################################
# tc_grpowned.rb
#
# Test case for the File.grpowned? class method. Some tests
# skipped on MS Windows.
#####################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Grpowned_ClassMethod < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      @file1 = File.join(Dir.pwd, 'temp1.txt')
      @gid   = Etc.getpwnam(Etc.getlogin).gid unless WINDOWS
      touch(@file1)
   end

   def test_grpowned_basic
      assert_respond_to(File, :grpowned?)
      assert_nothing_raised{ File.grpowned?(@file1) }
   end

   unless WINDOWS
      def test_grpowned
         assert_equal(true, File.grpowned?(@file1))
         assert_equal(false, File.grpowned?('bogus'))
         if ROOT
            assert_equal(true, File.grpowned?('/etc/passwd'))
         else
            assert_equal(false, File.grpowned?('/etc/passwd'))
         end
      end
   end

   def test_grpowned_expected_errors
      assert_raises(ArgumentError){ File.grpowned? }
      
      unless WINDOWS
         assert_raises(TypeError){ File.grpowned?(1) }
         assert_raises(TypeError){ File.grpowned?(nil) }
         assert_raises(TypeError){ File.grpowned?(false) }
      end
   end

   def teardown
      remove_file(@file1)
      @file1 = nil
      @gid   = nil unless WINDOWS
   end
end
