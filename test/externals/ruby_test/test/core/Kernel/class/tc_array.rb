###############################################################################
# tc_array.rb
#
# Test case for the Kernel.Array module method.
###############################################################################
require 'test/unit'

class TC_Kernel_Array_ModuleMethod < Test::Unit::TestCase
   def setup
      @integer = 1
      @range   = 1..3
      @hash    = {'a' => 1, 'b' => 7}
   end

   def test_array_basic
      assert_respond_to(Kernel, :Array)
      assert_kind_of(Array, Array(@range))
   end

   def test_array
      assert_equal([1], Array(@integer))
      assert_equal([['a', 1], ['b', 7]], Array(@hash))
      assert_equal([1, 2, 3], Array(@range))
   end

   def test_array_edge_cases
      assert_equal([], Array(nil))
      assert_equal([true], Array(true))
      assert_equal([false], Array(false))
      assert_equal([0], Array(0))
   end

   def test_array_expected_errors
      assert_raise(ArgumentError){ Array() }
      assert_raise(ArgumentError){ Array(@integer, @hash) }
   end

   def teardown
      @integer = nil
      @range   = nil
      @hash    = nil
   end
end
