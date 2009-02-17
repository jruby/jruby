###############################################################################
# tc_inspect.rb
#
# Test case for the Array#inspect instance method. There's a custom inspect
# implementation in array.c, so that's what we test it instead of relying on
# the tests for Object#inspect.
###############################################################################
require 'test/unit'

class TC_Array_Inspect_InstanceMethod < Test::Unit::TestCase
   def setup
      @empty     = []
      @normal    = [1, 'a', 2]
      @recursive = [1, 'a', 2]
      @recursive << @recursive
   end

   def test_inspect_basic
      assert_respond_to(@empty, :inspect)
      assert_nothing_raised{ @empty.inspect }
      assert_kind_of(String, @empty.inspect)
   end

   def test_inspect_empty
      assert_equal('[]', @empty.inspect)
   end

   def test_inspect_normal
      assert_equal('[1, "a", 2]', @normal.inspect)
   end

   def test_inspect_recursive
      assert_equal('[1, "a", 2, [...]]', @recursive.inspect)
   end

   def test_inspect_expected_errors
      assert_raise(ArgumentError){ @normal.inspect(true) }
   end

   def teardown
      @empty     = nil
      @normal    = nil
      @recursive = nil
   end
end
