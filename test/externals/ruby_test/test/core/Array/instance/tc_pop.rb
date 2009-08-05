################################################################
# tc_pop.rb
#
# Test suite for the Array#pop instance method.
################################################################
require "test/unit"

class TC_Array_Pop_Instance < Test::Unit::TestCase
   def setup
      @array = %w/a b c/
   end

   def test_pop_basic
      assert_respond_to(@array, :pop)
      assert_nothing_raised{ @array.pop }
   end

   def test_pop
      assert_equal("c", @array.pop)
      assert_equal("b", @array.pop)
      assert_equal("a", @array.pop)
      assert_equal(nil, @array.pop)
   end

   def test_pop_expected_errors
   # No longer a valid test in 1.8.7
=begin
      assert_raises(ArgumentError){ @array.pop("foo") }
=end
   end

   def teardown
      @array = nil
   end
end
