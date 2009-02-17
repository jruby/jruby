######################################################################
# tc_tell.rb
#
# Test case for the Dir#tell instance method.  This also covers the
# Dir#pos synonym.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_Dir_Tell_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @pwd = pwd_n
      @dir = Dir.new(@pwd)
   end

   def test_tell_basic
      assert_respond_to(@dir, :tell)
      assert_nothing_raised{ @dir.tell }
      assert_kind_of(Integer, @dir.tell)
   end

   def test_pos_basic
      assert_respond_to(@dir, :pos)
      assert_nothing_raised{ @dir.pos }
      assert_kind_of(Integer, @dir.pos)
   end

   def test_tell_expected_errors
      assert_raises(ArgumentError){ @dir.tell(1) }
   end

   def test_pos_expected_errors
      assert_raises(ArgumentError){ @dir.pos(1) }
   end

   def teardown
      @pwd = nil
      @dir = nil
   end
end
