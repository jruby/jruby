######################################################################
# tc_link.rb
#
# Test case for the File.link class method. Most tests skipped on
# MS Windows
######################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Link_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @file = File.expand_path(File.dirname(__FILE__)) + "/test.txt"
      @link = File.expand_path(File.dirname(__FILE__)) + "/test.lnk"
      touch(@file)
   end

   def test_link_basic
      assert_respond_to(File, :link)
      assert_nothing_raised{ File.link(@file, @link) } unless WINDOWS
   end

   unless WINDOWS
      def test_link
         assert_equal(0, File.link(@file, @link))
         assert_equal(true, File.exists?(@link))
      end

      def test_link_expected_errors
         assert_nothing_raised{ File.link(@file, @link) }
         assert_raises(Errno::EEXIST){ File.link(@file, @link) }
         assert_raises(ArgumentError){ File.link }
         assert_raises(ArgumentError){ File.link(@file) }
      end
   end

   def teardown
      remove_file(@link)
      remove_file(@file)
      @link = nil
   end
end
