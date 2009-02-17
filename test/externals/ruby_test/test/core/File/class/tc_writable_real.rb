#####################################################################
# tc_writable_real.rb
#
# Test case for the File.writable_real? class method. Some tests
# skipped on MS Windows.
#####################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Writable_Real_ClassMethod < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      @file1 = File.join(Dir.pwd, 'temp1.txt')
      @file2 = File.join(Dir.pwd, 'temp2.txt')
      @uid   = Etc.getpwnam('nobody').uid unless WINDOWS

      touch(@file1)
      touch(@file2)
      
      File.chmod(0644, @file1)
      File.chmod(0444, @file2)
   end

   def test_writable_real_basic
      assert_respond_to(File, :writable_real?)
      assert_nothing_raised{ File.writable_real?(@file1) }
   end

   unless WINDOWS
      if ROOT
         def test_writable_real_with_changed_uid
            assert_nothing_raised{ Process.uid = @uid }
            assert_equal(false, File.writable_real?(@file1))
            assert_equal(false, File.writable_real?(@file2))
            assert_equal(false, File.writable_real?('bogus'))
         end
      else
         def test_writable_real
            assert_equal(true, File.writable_real?(@file1))
            assert_equal(false, File.writable_real?(@file2))
            assert_equal(false, File.writable_real?('bogus'))
         end
      end
   end

   def test_writable_real_expected_errors
      assert_raises(ArgumentError){ File.writable_real? }
      assert_raises(TypeError){ File.writable_real?(1) }
      assert_raises(TypeError){ File.writable_real?(nil) }
      assert_raises(TypeError){ File.writable_real?(false) }
   end

   def teardown
      remove_file(@file1)
      remove_file(@file2)

      @file1 = nil
      @file2 = nil
      @uid   = nil unless WINDOWS
   end
end
