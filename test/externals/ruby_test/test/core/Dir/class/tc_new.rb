##############################################
# tc_new.rb
#
# Test suite for the Dir.new class method.
##############################################
require "test/unit"
require "test/helper"

class TC_Dir_New_Class < Test::Unit::TestCase
   include Test::Helper

   def setup
      @dir = base_file(__FILE__, "test")
      Dir.mkdir(@dir)
   end
   
   def test_new_basic
      assert_respond_to(Dir, :new)
      assert_nothing_raised{ Dir.new(@dir) }
      assert_kind_of(Dir, Dir.new(@dir))
   end

   # Dir.open with no block is a synonym for Dir.new
   def test_new_alias
      assert_respond_to(Dir, :open)
      assert_nothing_raised{ Dir.open(@dir) }
      assert_kind_of(Dir, Dir.open(@dir))
   end
   
   def test_new_expected_errors
      assert_raise(ArgumentError){ Dir.new }
      assert_raise(TypeError){ Dir.new(nil) }
      assert_raise(TypeError){ Dir.new(true) }
      assert_raise_kind_of(SystemCallError){ Dir.new("bogus") }
   end
   
   def teardown
      Dir.rmdir(@dir) if File.exists?(@dir)
   end
end
