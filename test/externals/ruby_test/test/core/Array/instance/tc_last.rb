#######################################################################
# tc_last.rb
#
# Test suite for the Array#last instance method.
#######################################################################
require 'test/unit'

class TC_Array_Last_InstanceMethod < Test::Unit::TestCase
   def setup
      @array = %w/q r s t/
   end

   def test_last_basic
      assert_respond_to(@array, :last)
      assert_nothing_raised{ @array.last }
      assert_nothing_raised{ @array.last(1) }
   end

   def test_last
      assert_equal('t', @array.last)
      assert_equal(['t'], @array.last(1))
      assert_equal(['r','s','t'], @array.last(3))
   end

   def test_last_with_float_index
      assert_equal(['t'], @array.last(1.2))
      assert_equal(['r','s','t'], @array.last(3.9))
   end

   def test_last_edge_cases
      assert_equal([], @array.last(0))
      assert_equal(['q', 'r', 's', 't'], @array.last(99))
      assert_equal([nil], [nil].last(1))
      assert_nil([].last)
   end

   def test_last_expected_errors
      assert_raises(TypeError){ @array.last('foo') }
      assert_raises(TypeError){ @array.last(nil) }
      assert_raises(TypeError){ @array.last(false) }
      assert_raises(TypeError){ @array.last(true) }
      assert_raises(ArgumentError){ @array.last(1,2) }
      assert_raises(ArgumentError){ @array.last(-1) }
   end

   def teardown
      @array = nil
   end
end
