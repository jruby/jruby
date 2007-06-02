######################################################################
# tc_truncate.rb
#
# Test case for the File.truncate class method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Truncate_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @fname1 = "test.txt"
      @file1  = File.open(@fname1, 'w')
      File.open(@fname1,"w"){ |fh| fh.write("1234567890") }
   end

   def test_truncate_basic
      assert_respond_to(File, :truncate)
      assert_nothing_raised{ File.truncate(@fname1, 0) }
      assert_equal(0, File.truncate(@fname1, 0))
   end

   def test_truncate
      assert_equal(10, File.size(@fname1))
      assert_nothing_raised{ File.truncate(@fname1, 5) }
      assert_equal(5, File.size(@fname1))
      assert_equal("12345", IO.read(@fname1))
   end

   def test_truncate_larger_than_original_file
      assert_nothing_raised{ File.truncate(@fname1, 12) }
      assert_equal("1234567890\000\000", IO.read(@fname1))
   end

   def test_truncate_same_size_as_original_file
      assert_nothing_raised{ File.truncate(@fname1, File.size(@fname1)) }
      assert_equal("1234567890", IO.read(@fname1))
   end

   def test_truncate_zero
      assert_nothing_raised{ File.truncate(@fname1, 0) }
      assert_equal("", IO.read(@fname1))
   end

   # There is a bug in the chsize() in MS VC++ 6.0 that may cause these
   # assertions to fail. They are fixed in MS VC++ 8.0.
   # 
   def test_truncate_expected_errors
      assert_raises(ArgumentError){ File.truncate(@fname1) }
      assert_raises(Errno::EINVAL){ File.truncate(@fname1, -1) } # May fail
      assert_raises(TypeError){ File.truncate(@fname1, nil) }
   end

   def teardown
      @file1.close rescue nil
      @fname1 = nil
   end
end
