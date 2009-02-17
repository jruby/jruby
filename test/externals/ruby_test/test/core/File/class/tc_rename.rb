########################################################################
# tc_rename.rb
#
# Test case for the File.rename class method.
########################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Rename_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @old_name = 'tc_rename.txt'
      @new_name = 'tc_rename_new.txt'
      touch(@old_name, 'File.rename test')
   end

   def test_rename_basic
      assert_respond_to(File, :rename)
      assert_nothing_raised{ File.rename(@old_name, @new_name) }
   end

   def test_rename
      assert_equal(0, File.rename(@old_name, @new_name))
      assert_equal(true, File.exists?(@new_name))
      assert_equal(false, File.exists?(@old_name))
      assert_equal(17, File.size(@new_name))
   end

   def test_rename_to_same_name
      assert_equal(0, File.rename(@old_name, @old_name))
      assert_equal(true, File.exists?(@old_name))
      assert_equal(17, File.size(@old_name))
   end

   def test_rename_expected_errors
      assert_raise(ArgumentError){ File.rename(@old_name) }
      assert_raise(ArgumentError, Errno::ENOENT){ File.rename(@old_name, '') }
      assert_raise(ArgumentError, Errno::ENOENT){ File.rename('bogus.txt', @new_name) }
      assert_raise(ArgumentError){ File.rename(@old_name, @new_name, 'test') }
      assert_raise(TypeError){ File.rename(@old_name, 1) }
   end

   def teardown
      File.delete(@old_name) if File.exists?(@old_name)
      File.delete(@new_name) if File.exists?(@new_name)
      @old_name = nil
      @new_name = nil
   end
end
