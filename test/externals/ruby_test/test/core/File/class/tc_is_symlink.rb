########################################################################
# tc_is_symlink.rb
#
# Test case for the File.symlink? class method.
#
# TODO: This will have to be updated for Vista.
########################################################################
require 'test/unit'
require 'test/helper'

class TC_File_IsSymlink_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @file = 'tc_is_symlink.txt'
      @link = 'link_to_tc_is_symlink.txt'

      touch(@file)

      File.symlink(@file, @link) unless WINDOWS
   end

   def test_is_symlink_basic
      assert_respond_to(File, :symlink?)
   end

   def test_is_symlink
      if WINDOWS
         assert_equal(false, File.symlink?(@file))
         assert_equal(false, File.symlink?(@link))
      else
         assert_equal(false, File.symlink?(@file))
         assert_equal(true, File.symlink?(@link))
      end
   end

   # The TypeError check will have to be updated for Vista
   def test_is_symlink_expected_errors
      assert_raise(ArgumentError){ File.symlink? }
      assert_raise(ArgumentError){ File.symlink?(@file, @link) }
      assert_raise(TypeError){ File.symlink?(1) } unless WINDOWS
   end

   def teardown
      File.delete(@link) if File.exists?(@link)
      File.delete(@file) if File.exists?(@file)
   end
end
