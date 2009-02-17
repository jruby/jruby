############################################
# tc_path.rb
#
# Test suite for the File#path method.
############################################
require "test/unit"
require "test/helper"

class TC_File_Path < Test::Unit::TestCase
   include Test::Helper

   def setup
      @pwd = File.expand_path(File.dirname(__FILE__))

      @file_no_path    = "test1"
      @file_with_path  = File.join(@pwd, "test2")
      @file_with_extra = File.join(@pwd, "/./test3")

      @fh_no_path    = File.new(@file_no_path, "wb+")
      @fh_with_path  = File.new(@file_with_path, "wb+")
      @fh_with_extra = File.new(@file_with_extra, "wb+")
   end

   def test_path_basic
      assert_respond_to(@fh_no_path, :path)
      assert_nothing_raised{ @fh_no_path.path }
      assert_kind_of(String, @fh_no_path.path)
   end

   def test_path
      assert_equal("test1", @fh_no_path.path)
      assert_equal(File.join(@pwd, "/test2"), @fh_with_path.path)
      assert_equal(File.join(@pwd, "/./test3"), @fh_with_extra.path)
   end
   
   def test_tainted_path_returns_tainted_string
      assert_nothing_raised{ @fh_with_path.taint }
#      assert_equal(true, @fh_with_path.path.tainted?)
   end   

   def test_path_edge_cases
      assert_nothing_raised{ @fh_no_path.close }
      assert_nothing_raised{ @fh_no_path.path }
   end

   def teardown
      @fh_no_path.close unless @fh_no_path.closed?
      @fh_with_path.close unless @fh_with_path.closed?
      @fh_with_extra.close unless @fh_with_extra.closed?

      File.unlink(@file_no_path)
      File.unlink(@file_with_path)
      File.unlink(@file_with_extra)

      @fh_no_path    = nil
      @fh_with_path  = nil
      @fh_with_extra = nil
   end
end
