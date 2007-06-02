#####################################################################
# tc_next.rb
#
# Test case for the Integer#next method.
#####################################################################
require 'test/unit'

class TC_Integer_Next_Instance < Test::Unit::TestCase
   def setup
      @int1 = 65
   end

   def test_next_basic
      assert_respond_to(@int1, :next)
      assert_respond_to(@int1, :next)
   end

   def test_next
      assert_nothing_raised{ @int1.next }
      assert_equal(66, @int1.next)
      assert_equal(66, @int1.next)
   end

   def test_succ_alias
      assert_nothing_raised{ @int1.succ }
      assert_equal(66, @int1.succ)
      assert_equal(66, @int1.succ)
   end

   def test_next_expected_errors
      assert_raises(ArgumentError){ @int1.next(2) }
   end

   def teardown
      @int1 = nil
   end
end
