#####################################################################
# tc_round.rb
#
# Test case for the Integer#round method.
#####################################################################
require 'test/unit'

class TC_Integer_Round_Instance < Test::Unit::TestCase
   def setup
      @int1 = 65
   end

   def test_round_basic
      assert_respond_to(@int1, :round)
   end

   def test_round
      assert_nothing_raised{ @int1.round }
      assert_equal(@int1, @int1.round)

      assert_equal(0, 0.round)
      assert_equal(-1, -1.round)
      assert_equal(1, 1.round)
   end

   def test_round_expected_errors
      assert_raises(ArgumentError){ @int1.round(2) }
   end

   def teardown
      @int1 = nil
   end
end
