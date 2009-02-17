######################################################################
# tc_truncate.rb
#
# Test case for the File#truncate instance method.
######################################################################
require 'test/unit'

class TC_File_Truncate_InstanceMethod < Test::Unit::TestCase

   def setup
      @name = 'test_instance_truncate.txt'
      @file = File.open(@name, 'wb')
      @file.write('1234567890')
   end

   def test_truncate_basic
      assert_respond_to(@file, :truncate)
      assert_equal(0, @file.truncate(0))
   end

   def test_truncate
      assert_nothing_raised{ @file.close }
      assert_equal(10, File.size(@name))
      assert_nothing_raised{ File.open(@name, 'ab'){ |fh| fh.truncate(5) } }
      assert_equal(5, File.size(@name))
      assert_equal('12345', IO.read(@name))
   end

   def test_truncate_larger_than_original_file
      assert_nothing_raised{ @file.close }
      assert_equal(10, File.size(@name))
      assert_nothing_raised{ File.open(@name, 'ab'){ |fh| fh.truncate(12) } }
      assert_equal(12, File.size(@name))
      assert_equal("1234567890\000\000", IO.read(@name))
   end

   def test_truncate_same_size_as_original_file
      assert_nothing_raised{ @file.close }
      assert_equal(10, File.size(@name))
      assert_nothing_raised{ File.open(@name, 'ab'){ |fh| fh.truncate(10) } }
      assert_equal(10, File.size(@name))
      assert_equal("1234567890", IO.read(@name))
   end

   def test_truncate_zero
      assert_nothing_raised{ @file.close }
      assert_equal(10, File.size(@name))
      assert_nothing_raised{ File.open(@name, 'ab'){ |fh| fh.truncate(0) } }
      assert_equal(0, File.size(@name))
      assert_equal("", IO.read(@name))
   end

   # There is a bug in the chsize() in MS VC++ 6.0 that may cause these
   # assertions to fail. They are fixed in MS VC++ 8.0.
   # 
   def test_truncate_expected_errors
      msg = '=> Known issue with on MS Windows/VC++ 6 and earlier'
      assert_raise(ArgumentError){ @file.truncate }
      assert_raise(Errno::EINVAL, msg){ @file.truncate(-1) }
      assert_raise(TypeError){ @file.truncate(nil) }
   end

   def teardown
      @file.close unless @file.closed?
      File.delete(@name) if File.exists?(@name)
      @file = nil
      @name = nil
   end
end
