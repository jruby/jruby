########################################################################
# tc_min.rb
#
# Test case for the Enumerable#min instance method.
#
# Note: I use arrays here because I know that array.c doesn't implement
# its own version.
########################################################################
require 'test/unit'

class TC_Enumerable_Min_InstanceMethod < Test::Unit::TestCase
   def setup
      @enum_nums  = [1, 2, 3]
      @enum_alpha = ['alpha', 'beta', 'gamma']
   end

   def test_min_basic
      assert_equal(true, @enum_nums.respond_to?(:min))
      assert_nothing_raised{ @enum_nums.min }
      assert_nothing_raised{ @enum_nums.min{ |a,b| a <=> b } }
   end

   def test_min
      assert_equal(1, @enum_nums.min)
      assert_equal('alpha', @enum_alpha.min)
   end

   def test_min_with_block
      assert_equal(3, @enum_nums.min{ |a,b| b <=> a} )
      assert_equal('gamma', @enum_alpha.min{ |a,b| b <=> a} )
      assert_equal('beta', @enum_alpha.min{ |a,b| a.length <=> b.length } )
   end

   def test_min_edge_cases
      assert_equal(nil, [].min)
      assert_equal(nil, [nil].min)
   end

   def test_min_expected_errors
      assert_raise(NoMethodError){ [nil, nil].min }
   end

   def teardown
      @enum_nums  = nil
      @enum_alpah = nil
   end
end
