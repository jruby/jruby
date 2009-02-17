######################################################################
# tc_rewind.rb
#
# Test case for the Dir#rewind instance method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_Dir_Rewind_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @pwd = pwd_n
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
