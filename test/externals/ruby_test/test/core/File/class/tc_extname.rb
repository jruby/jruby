###########################################
# tc_extname.rb
#
# Test suite for the File.extname method.
###########################################
require "test/unit"

class TC_File_Extname < Test::Unit::TestCase
   def setup
      @file = "foo.rb"
   end

   def test_extname_basic
      assert_respond_to(File, :extname)
      assert_nothing_raised{ File.extname("foo.rb") }
      assert_kind_of(String, File.extname("foo.rb"))
   end

   def test_extname_unix
      assert_equal(".rb", File.extname("foo.rb"))
      assert_equal(".rb", File.extname("/foo/bar.rb"))
      assert_equal(".c", File.extname("/foo.rb/bar.c"))
      assert_equal("", File.extname("bar"))
      assert_equal("", File.extname(".bashrc"))
      assert_equal("", File.extname("/foo.bar/baz"))
      assert_equal(".conf", File.extname(".app.conf"))     
   end

   def test_tainted_ext_returns_tainted_string
      assert_equal(false, File.extname(@file).tainted?)
      assert_nothing_raised{ @file.taint }
#      assert_equal(true, File.extname(@file).tainted?)
   end

   def test_extname_edge_cases
      assert_equal("", File.extname(""))
      assert_equal("", File.extname("."))
      assert_equal("", File.extname("/"))
      assert_equal("", File.extname("/."))
      assert_equal("", File.extname(".."))
      assert_equal("", File.extname(".foo."))
      assert_equal("", File.extname("foo."))
   end

   def test_extname_expected_errors
      assert_raises(TypeError){ File.extname(nil) }
      assert_raises(TypeError){ File.extname(0) }
      assert_raises(TypeError){ File.extname(true) }
      assert_raises(TypeError){ File.extname(false) }
      assert_raises(ArgumentError){ File.extname("foo.bar", "foo.baz") }
   end
end
