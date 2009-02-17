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
      @file = 'test_truncate.txt'
      @fh  = File.open(@file, 'w')
      File.open(@file, 'wb'){ |fh| fh.write("1234567890") }
   end

   def test_truncate_basic
      assert_respond_to(File, :truncate)
      assert_nothing_raised{ File.truncate(@file, 0) }
      assert_equal(0, File.truncate(@file, 0))
   end

   def test_truncate
      assert_equal(10, File.size(@file))
      assert_nothing_raised{ File.truncate(@file, 5) }
      assert_equal(5, File.size(@file))
      assert_equal("12345", IO.read(@file))
   end

   def test_truncate_larger_than_original_file
      assert_nothing_raised{ File.truncate(@file, 12) }
      assert_equal("1234567890\000\000", IO.read(@file))
   end

   def test_truncate_same_size_as_original_file
      assert_nothing_raised{ File.truncate(@file, File.size(@file)) }
      assert_equal("1234567890", IO.read(@file))
   end

   def test_truncate_zero
      assert_nothing_raised{ File.truncate(@file, 0) }
      assert_equal("", IO.read(@file))
   end

   # There is a bug in the chsize() in MS VC++ 6.0 that may cause these
   # assertions to fail. They are fixed in MS VC++ 8.0.
   # 
   def test_truncate_expected_errors
      msg = '=> Known issue with on MS Windows/VC++ 6 and earlier'
      assert_raise(ArgumentError){ File.truncate(@file) }
      assert_raise(Errno::EINVAL, msg){ File.truncate(@file, -1) }
      assert_raise(TypeError){ File.truncate(@file, nil) }
   end

   def teardown
      @fh.close if @fh && !@fh.closed?
      File.delete(@file) if File.exists?(@file)
      @file = nil
      @fh = nil
   end
end
