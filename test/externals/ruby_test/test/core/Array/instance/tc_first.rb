#######################################################################
# tc_first.rb
#
# Test case for the Array#first instance method.
#######################################################################
require 'test/unit'

class TC_Array_First_InstanceMethod < Test::Unit::TestCase
   def setup
      @array = %w/q r s t/
   end

   def test_first_basic
      assert_respond_to(@array, :first)
      assert_nothing_raised{ @array.first }
      assert_nothing_raised{ @array.first(1) }
   end

   def test_first_results
      assert_equal('q', @array.first)
      assert_equal(['q'], @array.first(1))
      assert_equal(['q','r','s'], @array.first(3))
   end

   def test_first_with_float
      assert_equal(['q'], @array.first(1.5))
      assert_equal(['q','r','s'], @array.first(3.9))
   end

   def test_first_edge_cases
      assert_equal([], @array.first(0))
      assert_equal(['q','r','s','t'], @array.first(99))
      assert_equal([nil], [nil].first(1))
      assert_nil([].first)
   end

   def test_first_expected_errors
      assert_raises(TypeError){ @array.first('foo') }
      assert_raises(TypeError){ @array.first(nil) }
      assert_raises(TypeError){ @array.first(false) }
      assert_raises(TypeError){ @array.first(true) }
      assert_raises(TypeError){ @array.first(1..3) }
      assert_raises(ArgumentError){ @array.first(1,2) }
      assert_raises(ArgumentError){ @array.first(-1) }
   end

   def teardown
      @array = nil
   end
end
