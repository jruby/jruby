########################################################################
# tc_symlink.rb
#
# Test case for the File.symlink class method.
#
# TODO: Most of these tests are skipped on MS Windows, but will need
# to be updated for Vista.
########################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Symlink_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @file = 'tc_symlink.txt'
      @link = 'link_to_tc_symlink.txt'

      touch(@file)
   end

   def test_symlink_basic
      assert_respond_to(File, :symlink)
   end

   unless WINDOWS
      def test_symlink
         assert_nothing_raised{ File.symlink(@file, @link) }
         assert_equal(true, File.exists?(@link))
         assert_equal(true, File.symlink?(@link))
      end

      def test_link_already_exists
         assert_nothing_raised{ File.symlink(@file, @link) }
         assert_raise(Errno::EEXIST){ File.symlink(@link, @link) }
      end

      def test_symlink_expected_errors
         assert_raise(ArgumentError){ File.symlink }
         assert_raise(ArgumentError){ File.symlink(@file) }
         assert_raise(ArgumentError){ File.symlink(@file, @link, @link) }
      end
   end

   # Be sure to delete the link first
   def teardown
      File.delete(@link) if File.exists?(@link)
      File.delete(@file) if File.exists?(@file)

      @file = nil
      @link = nil
   end
end
