########################################################################
# tc_to_i.rb
#
# Test case for the Integer#to_i method and the aliases, Integer#ceil,
# Integer#round, Integer#to_int, and Integer#truncate.
########################################################################
require 'test/unit'

class TC_Integer_ToI_Instance < Test::Unit::TestCase
   def setup
      @int1 = 65
   end

   def test_to_i_basic
      assert_respond_to(@int1, :to_i)
   end

   def test_to_i
      assert_nothing_raised{ @int1.to_i }
      assert_equal(@int1, @int1.to_i)

      assert_equal(0, 0.to_i)
      assert_equal(-1, -1.to_i)
      assert_equal(1, 1.to_i)
   end

   def test_to_int_alias
      assert_nothing_raised{ @int1.to_int }
      assert_equal(@int1, @int1.to_int)

      assert_equal(0, 0.to_int)
      assert_equal(-1, -1.to_int)
      assert_equal(1, 1.to_int)
   end

   def test_truncate_alias
      assert_nothing_raised{ @int1.truncate }
      assert_equal(@int1, @int1.truncate)

      assert_equal(0, 0.truncate)
      assert_equal(-1, -1.truncate)
      assert_equal(1, 1.truncate)
   end

   def test_ceil_alias
      assert_nothing_raised{ @int1.ceil }
      assert_equal(@int1, @int1.ceil)

      assert_equal(0, 0.ceil)
      assert_equal(-1, -1.ceil)
      assert_equal(1, 1.ceil)
   end

   def test_round_alias
      assert_nothing_raised{ @int1.round }
      assert_equal(@int1, @int1.round)

      assert_equal(0, 0.round)
      assert_equal(-1, -1.round)
      assert_equal(1, 1.round)
   end

   def test_to_i_expected_errors
      assert_raises(ArgumentError){ @int1.to_i(2) }
   end

   def teardown
      @int1 = nil
   end
end
