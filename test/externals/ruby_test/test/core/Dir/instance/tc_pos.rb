######################################################################
# tc_pos.rb
#
# Test case for the Dir#pos= instance method.  Note that Dir#pos is
# tested in the tc_tell test case since it's a synonym.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_Dir_Pos_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @pwd = pwd_n
      @dir = Dir.new(@pwd)
   end

   def test_pos_basic
      assert_respond_to(@dir, :pos=)
      assert_nothing_raised{ @dir.pos = 0 }
   end

   def test_pos
      assert_equal(".", @dir.read)
      assert_equal(1, @dir.pos = 1)
   end

   def test_pos_expected_errors
      assert_raises(TypeError){ @dir.pos = "bogus"  }
   end

   def teardown
      @pwd = nil
      @dir = nil
   end
end
