###############################################################
# tc_getwd.rb
#
# Test suite for the Dir.getwd class method.  This also tests
# the Dir.pwd alias.
###############################################################
require "test/unit"

class TC_Dir_Getwd_Class < Test::Unit::TestCase
   def setup
      @pwd = `pwd`.chomp
      @pwd.tr!('\\','/') if PLATFORM.match("mswin")
   end

   def test_getwd_basic
      assert_respond_to(Dir, :getwd)
      assert_nothing_raised{ Dir.getwd }
      assert_kind_of(String, Dir.getwd)
   end

   def test_getwd
      assert_equal(@pwd, Dir.getwd)
   end
   
   # Alias
   def test_pwd_basic
      assert_respond_to(Dir, :pwd)
      assert_nothing_raised{ Dir.pwd }
      assert_kind_of(String, Dir.pwd)
   end

   def test_pwd
      assert_equal(@pwd, Dir.pwd)
   end
   
   def test_getwd_expected_errors
      assert_raises(ArgumentError){ Dir.getwd("foo") }
   end

   def teardown
      @pwd = nil
   end
end
