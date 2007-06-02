###########################################################
# tc_fill.rb
#
# Test suite for the Array#fill instance method.
###########################################################
require "test/unit"

class TC_Array_Fill_Instance < Test::Unit::TestCase
   def setup
      @array = %w/a b c d/
   end

   def test_fill_basic
      assert_respond_to(@array, :fill)
      assert_nothing_raised{ @array.fill("test") }
      assert_nothing_raised{ @array.fill("test", 1) }
      assert_nothing_raised{ @array.fill("test", 1, 2) }
      assert_nothing_raised{ @array.fill("test", 1..2) }
      assert_nothing_raised{ @array.fill{ "test" } }
      assert_nothing_raised{ @array.fill(1){ "test" } }
      assert_nothing_raised{ @array.fill(1, 2){ "test" } }
   end

   def test_fill
      assert_equal(["x","x","x","x"], @array.fill("x"))
      assert_equal(["x","x","z","z"], @array.fill("z", 2))
      assert_equal(["y","y","z","z"], @array.fill("y", 0, 2))
      assert_equal(["v","v","z","z"], @array.fill("v", 0..1))
      assert_equal(["v","a","a","a"], @array.fill("a", -3))
      assert_equal(["v","a","a","a"], @array.fill("b", 99))
   end

   def test_fill_with_block
      assert_equal(["x","x","x","x"], @array.fill{ "x" })
      assert_equal(["x","x","z","z"], @array.fill(2){ "z" })
      assert_equal(["y","y","z","z"], @array.fill(0,2){ "y" })
      assert_equal(["v","v","z","z"], @array.fill(0..1){ "v" })
   end

   def test_fill_edge_cases
      assert_equal([], [].fill("x"))
      assert_equal([], [].fill([]))
   end

   def test_expected_errors
      assert_raises(ArgumentError){ @array.fill }
   end

   def teardown
      @array = nil
   end
end
