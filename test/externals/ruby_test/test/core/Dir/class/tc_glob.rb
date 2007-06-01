######################################################################
# tc_glob.rb
#
# Test case for the Dir.glob class method.
######################################################################
require "test/unit"

class TC_Dir_Glob_Class < Test::Unit::TestCase
   def setup
   end

   def test_glob_basic
      assert_respond_to(Dir, :glob)
      assert_nothing_raised{ Dir.glob("*") }
   end

   def test_glob
      assert_nothing_raised{ Dir.glob("**") }
      assert_nothing_raised{ Dir.glob("foo.*") }
      assert_nothing_raised{ Dir.glob("foo.?") }
      assert_nothing_raised{ Dir.glob("*.[^r]*") }
      assert_nothing_raised{ Dir.glob("*.[a-z][a-z]") }
      assert_nothing_raised{ Dir.glob("*.{rb,h}") }
      assert_nothing_raised{ Dir.glob("*.\t") }
   end

   def test_glob_flags
      assert_nothing_raised{ Dir.glob("*", File::FNM_DOTMATCH) }
      assert_nothing_raised{ Dir.glob("*", File::FNM_NOESCAPE) }
      assert_nothing_raised{ Dir.glob("*", File::FNM_PATHNAME) }
      assert_nothing_raised{ Dir.glob("*", File::FNM_CASEFOLD) }
   end

   def test_glob_expected_errors
      assert_raises(TypeError){ Dir.glob("*", "*") }
   end

   def teardown
   end
end
