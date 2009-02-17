########################################################################
# tc_max.rb
#
# Test case for the Enumerable#max instance method.
#
# Note: I use arrays here because I know that array.c doesn't implement
# its own version.
########################################################################
require 'test/unit'

class TC_Enumerable_Max_InstanceMethod < Test::Unit::TestCase
   def setup
      @enum_nums  = [1, 2, 3]
      @enum_alpha = ['alpha', 'beetlejuice', 'gamma']
   end

   def test_max_basic
      assert_equal(true, @enum_nums.respond_to?(:max))
      assert_nothing_raised{ @enum_nums.max }
      assert_nothing_raised{ @enum_nums.max{ |a,b| a <=> b } }
   end

   def test_max
      assert_equal(3, @enum_nums.max)
      assert_equal('gamma', @enum_alpha.max)
   end

   def test_max_with_block
      assert_equal(1, @enum_nums.max{ |a,b| b <=> a} )
      assert_equal('alpha', @enum_alpha.max{ |a,b| b <=> a} )
      assert_equal('beetlejuice', @enum_alpha.max{ |a,b| a.length <=> b.length } )
   end

   def test_max_edge_cases
      assert_equal(nil, [].max)
      assert_equal(nil, [nil].max)
   end

   def test_max_expected_errors
      assert_raise(NoMethodError){ [nil, nil].max }
   end

   def teardown
      @enum_nums  = nil
      @enum_alpah = nil
   end
end
