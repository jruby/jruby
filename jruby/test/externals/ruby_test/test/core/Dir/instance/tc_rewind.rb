######################################################################
# tc_rewind.rb
#
# Test case for the Dir#rewind instance method.
######################################################################
require "test/unit"

class TC_Rewind_Dir_Instance < Test::Unit::TestCase
   def setup
      @pwd = `pwd`.chomp
      @dir = Dir.new(@pwd)
   end

   def test_rewind_basic
      assert_respond_to(@dir, :rewind)
      assert_nothing_raised{ @dir.rewind }
   end

   def test_rewind
      assert_equal(".", @dir.read)
      assert_nothing_raised{ @dir.rewind }
      assert_equal(".", @dir.read)
      assert_nothing_raised{ 5.times{ @dir.rewind } }
      assert_kind_of(Dir, @dir.rewind)
   end

   def test_rewind_expected_failures
      assert_raises(ArgumentError){ @dir.rewind(2) }
   end

   def teardown
      @pwd = nil
      @dir = nil
   end
end
