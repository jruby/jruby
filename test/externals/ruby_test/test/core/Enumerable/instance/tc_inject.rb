########################################################################
# tc_inject.rb
#
# Test case for the Enumerable#inject instance method.
########################################################################
require 'test/unit'

class TC_Enumerable_Inject_InstanceMethod < Test::Unit::TestCase
   def setup
      @memo       = nil
      @enum_nums  = [1, 2, 3]
      @enum_alpha = ['a', 'b', 'c']
   end

   def test_inject_basic
      assert_respond_to(@enum_nums, :inject)
      assert_nothing_raised{ @enum_nums.inject{} }
   end

   def test_inject
      assert_equal(6, @enum_nums.inject{ |m, n| m + n })
      assert_equal('abc', @enum_alpha.inject{ |m, n| m + n })
   end

   def test_inject_with_initial_value
      assert_equal(10, @enum_nums.inject(4){ |m, n| m + n })
      assert_equal('xxxabc', @enum_alpha.inject('xxx'){ |m, n| m + n })
   end

   def test_inject_edge_cases
      assert_equal(nil, [].inject{ |m,n| m + n })
      assert_equal(0, [0].inject{ |m,n| m + n })
   end

   def test_inject_expected_errors
      assert_raise(LocalJumpError){ @enum_nums.inject }
      assert_raise(ArgumentError){ @enum_nums.inject(1,2){} }
   end

   def teardown
      @memo       = nil
      @enum_nums  = nil
      @enum_alpha = nil
   end
end
