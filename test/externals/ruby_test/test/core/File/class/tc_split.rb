#####################################################
# tc_split.rb
#
# Test suite for the File.split method.
#####################################################
require "test/unit"

class TC_File_Split < Test::Unit::TestCase
   def setup
      @path_unix             = "/foo/bar/baz.rb"
      @path_windows_backward = "C:\\foo\\bar\\baz.rb"
      @path_windows_forward  = "C:/foo/bar/baz.rb"
   end

   def test_split_unix
      assert_equal(["/foo/bar","baz.rb"], File.split(@path_unix))
   end

   def test_split_edge_cases
   end

   def test_split_windows
   end

   def teardown
   end
end
