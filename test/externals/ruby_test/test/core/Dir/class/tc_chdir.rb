######################################################################
# tc_chdir.rb
#
# Test case for the Dir.chdir class method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_Dir_Chdir_Class < Test::Unit::TestCase
   include Test::Helper

   def setup
      @pwd      = Dir.pwd
      @old_home = get_home
      ENV["HOME"] = @pwd
   end

   def test_chdir_basic
      assert_respond_to(Dir, :chdir)
      assert_nothing_raised{ Dir.chdir }
      assert_nothing_raised{ Dir.chdir(@pwd) }
      assert_nothing_raised{ Dir.chdir(@pwd){} }
   end

   def test_chdir
      assert_equal(0, Dir.chdir(@pwd))
      assert_nothing_raised{ Dir.chdir }
      assert_equal(@pwd, Dir.pwd)
   end

   def test_chdir_block
      assert_nothing_raised{ Dir.chdir{ @old_home } }
      assert_equal(@pwd, Dir.pwd)
   end

   def test_chdir_expected_errors
      assert_raise(ArgumentError){ Dir.chdir(@pwd, @pwd) }
      assert_raise(TypeError){ Dir.chdir(1) }

      ENV["HOME"] = "bogus"
      assert_raise_kind_of(SystemCallError){ Dir.chdir }
   end

   def teardown
      @pwd.tr!('/', "\\") if WINDOWS
      WINDOWS ? system("chdir #{@pwd}") : system("cd #{@pwd}")
      ENV["HOME"] = @old_home
      @pwd = nil
   end
end
