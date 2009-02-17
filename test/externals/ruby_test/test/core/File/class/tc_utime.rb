#####################################################################
# tc_utime.rb
#
# Test case for the File.utime class method.
#####################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Utime_ClassMethod < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      @time  = Time.now
      @file1 = File.join(Dir.pwd, 'test_utime1.txt')
      @file2 = File.join(Dir.pwd, 'test_utime2.txt')
      
      touch(@file1)
      touch(@file2)
   end

   def test_utime_basic
      assert_respond_to(File, :utime)
      assert_nothing_raised{ File.utime(0, 0, @file1) }
      assert_nothing_raised{ File.utime(0, @time, @file1) }
      assert_nothing_raised{ File.utime(@time, @time, @file1) }
      assert_kind_of(Fixnum, File.utime(0, 0, @file1))
   end
   
   def test_utime
      assert_equal(1, File.utime(0, 0, @file1))
      assert_nothing_raised{ File.utime(@time, @time, @file1) }
      assert_equal(@time.to_s, File.mtime(@file1).to_s)
      assert_equal(@time.to_s, File.atime(@file1).to_s)
   end
   
   def test_utime_multiple_files
      assert_equal(2, File.utime(0, 0, @file1, @file2))
      assert_nothing_raised{ File.utime(@time, @time, @file1, @file2) }
      assert_equal(@time.to_s, File.mtime(@file1).to_s)
      assert_equal(@time.to_s, File.atime(@file1).to_s)
      assert_equal(@time.to_s, File.mtime(@file2).to_s)
      assert_equal(@time.to_s, File.atime(@file2).to_s)
   end
   
   def test_utime_edge_cases
      assert_equal(0, File.utime(0, 0)) # Bug?
   end

   def test_utime_expected_errors
      assert_raises(Errno::ENOENT){ File.utime(0, 0, 'bogus') }
      assert_raises(ArgumentError){ File.utime }
      assert_raises(ArgumentError){ File.utime(0) }
      assert_raises(TypeError){ File.utime('bogus', 'bogus') }
   end

   def teardown
      remove_file(@file1)
      remove_file(@file2)
      
      @file1 = nil
      @file2 = nil
      @time  = nil
   end
end
