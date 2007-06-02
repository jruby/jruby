#####################################################################
# tc_delete.rb
#
# Test case for the File.delete class method, and the File.unlink
# alias.
#####################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Delete_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @file1 = 'temp1.txt'
      @file2 = 'temp2.txt'

      File.open(@file1, "w"){} # Touch
      File.open(@file2, "w"){} # Touch
   end

   def test_delete_basic
      assert_respond_to(File, :delete)
      assert_nothing_raised{ File.delete(@file1) }
   end

   def test_delete
      assert_equal(0, File.delete)
      assert_equal(1, File.delete(@file1))
   end

   def test_delete_multiple_files
      assert_equal(2, File.delete(@file1, @file2))
   end

   def test_delete_expected_errors
      assert_raises(TypeError){ File.delete(1) }
      assert_raises(Errno::ENOENT){ File.delete('bogus') }
   end

   def test_unlink_alias
      assert_respond_to(File, :unlink)
      assert_nothing_raised{ File.unlink(@file1) }
   end

   def teardown
      remove_file(@file1)
      remove_file(@file2)

      @file1 = nil
      @file2 = nil
   end
end
