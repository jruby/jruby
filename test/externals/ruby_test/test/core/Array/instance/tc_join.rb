######################################################
# tc_join.rb
#
# Test suite for the Array#join instance method.
######################################################
require "test/unit"

class TC_Array_Join_Instance < Test::Unit::TestCase
   def setup
      @array  = %w/a b c/
      @nested = [1, [2, 3], ['a', 'b']]
   end

   def test_join_basic
      assert_respond_to(@array, :join)
      assert_nothing_raised{ @array.join }
   end

   def test_join
      assert_equal('abc', @array.join)
      assert_equal('a-b-c', @array.join('-'))
      assert_equal('abc', @array.join(nil))
      assert_equal('1-2-3-a-b', @nested.join('-'))
   end
   
   def test_join_edge_cases
      assert_equal('', [].join)
      assert_equal('', [[], []].join)
      assert_equal('', [nil].join)
      assert_equal('true-false', [true, false].join('-'))
   end

   def test_join_expected_errors
      assert_raises(TypeError){ @array.join(true) }
      assert_raises(ArgumentError){ @array.join(1,2) }
   end

   def teardown
      @array  = nil
      @nested = nil
   end
end
