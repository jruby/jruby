######################################################################
# tc_size.rb
#
# Test case for the File.size class method.
#
# TODO: Add big file (>2gb) tests.
#######################################################################
require 'test/unit'

class TC_File_Size_ClassMethod < Test::Unit::TestCase
   def setup
      @zero_file  = "zero.test"  # Size 0
      @small_file = "small.test" # Size > 0 < 2 GB
      @large_file = "large.test" # Size > 2 GB

      File.open(@zero_file, "wb+"){}
      File.open(@small_file, "wb+"){ |fh| 50.times{ fh.syswrite('hello') } }
   end

   def test_size_basic
      assert_respond_to(File, :size)
      assert_nothing_raised{ File.size(@zero_file) }
      assert_kind_of(Integer, File.size(@zero_file))
   end
   
   def test_size
      assert_equal(0, File.size(@zero_file))
      assert_equal(250, File.size(@small_file))
   end
   
   def test_size_expected_errors
      assert_raise(ArgumentError){ File.size }
      assert_raise(ArgumentError){ File.size(@zero_file, @small_file) }
   end

   def teardown
      File.unlink(@zero_file) if File.exists?(@zero_file)
      File.unlink(@small_file) if File.exists?(@small_file)
      File.unlink(@large_file) if File.exists?(@large_file)
   end
end
