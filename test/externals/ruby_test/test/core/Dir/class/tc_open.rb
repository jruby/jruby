######################################################################
# tc_open.rb
#
# Test case for the Dir.open class method.
######################################################################
require "test/unit"

class TC_Dir_Open_Class < Test::Unit::TestCase
   def setup
      @dir = "test"
      @pwd = Dir.pwd
   end

   def test_open_basic
      assert_respond_to(Dir, :open)
      assert_nothing_raised{ Dir.open(@pwd){} }
   end

   def test_open
      Dir.open(@pwd){ |dir|
         assert_kind_of(Dir, dir)
      }
   end

   def test_open_expected_errors
      assert_raises(TypeError){ Dir.open(1) }
      assert_raises(ArgumentError){ Dir.open(@pwd, @pwd) }
   end

   def teardown
      @dir = nil
      @pwd = nil
   end
end
