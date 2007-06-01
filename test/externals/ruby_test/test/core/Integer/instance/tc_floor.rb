#####################################################################
# tc_floor.rb
#
# Test case for the Integer#floor method.
#####################################################################
require 'test/unit'

class TC_Integer_Floor_Instance < Test::Unit::TestCase
   def setup
      @int1 = 65
   end

   def test_floor_basic
      assert_respond_to(@int1, :floor)
   end

   def test_floor
      assert_nothing_raised{ @int1.floor }
      assert_equal(@int1, @int1.floor)

      assert_equal(0, 0.floor)
      assert_equal(-1, -1.floor)
      assert_equal(1, 1.floor)
   end

   def test_floor_expected_errors
      assert_raises(ArgumentError){ @int1.floor(2) }
   end

   def teardown
      @int1 = nil
   end
end
