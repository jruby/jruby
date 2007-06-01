########################################################
# tc_assoc.rb
#
# Test suite for the Array#assoc instance method.
########################################################
require "test/unit"

class TC_Array_Assoc_Instance < Test::Unit::TestCase
   def setup
      @colors  = ["colors", "red", "blue", "green"]
      @letters = ["letters", "a", "b", "c"]
      @numbers = [1, 2, 3]
      @array1  = [@colors, @letters, @numbers]

      @nil    = [nil, nil, nil]
      @false  = [false, false, false]
      @zero   = [0, 0, 0]
      @array2 = [@nil, @false, @zero]
   end

   def test_assoc_basic
      assert_respond_to(@colors, :assoc)
      assert_nothing_raised{ @colors.assoc("colors") }
   end

   def test_assoc_results
      assert_equal(["colors", "red", "blue", "green"], @array1.assoc("colors"))
      assert_equal(["letters", "a", "b", "c"], @array1.assoc("letters"))
      assert_equal([1, 2, 3], @array1.assoc(1))
      assert_equal(nil, @array1.assoc("bogus"))
   end

   def test_assoc_edge_cases
      assert_equal([nil, nil, nil], @array2.assoc(nil))
      assert_equal([false, false, false], @array2.assoc(false))
      assert_equal([0, 0, 0], @array2.assoc(0))
   end

   def teardown
      @colors  = nil
      @letters = nil
      @numbers = nil
      @array1  = nil

      @nil    = nil
      @false  = nil
      @zero   = nil
      @array2 = nil
   end
end
