########################################################################
# tc_threequals.rb
#
# Test case for the Module#=== instance method.
########################################################################
require 'test/unit'
require 'test/helper'

module ThreeModA; end
module ThreeModB; end
module ThreeModC; end

class ThreeClass
   include ThreeModA
   include ThreeModB
end

class TCompare_Module_Threequals_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @obj = ThreeClass.new
   end

   def test_threequals_basic
      assert_respond_to(ThreeModA, :===)
      assert_nothing_raised{ ThreeModA === ThreeModB }
      assert_kind_of(Boolean, ThreeModA === ThreeModB)
   end

   def test_threequals
      assert_equal(true, ThreeModA === @obj)
      assert_equal(true, ThreeModB === @obj)
      assert_equal(false, ThreeModC === @obj)
   end

   # Oops?
   def test_threequals_commutative
      assert_equal(false, @obj === ThreeModA)
      assert_equal(false, @obj === ThreeModB)
      assert_equal(false, @obj === ThreeModC)
   end

   def test_threequals_edge_cases
      assert_equal(false, @obj === 'a')
      assert_equal(false, @obj === 1)
      assert_equal(false, @obj === nil)
   end

   def test_threequals_expected_errors
      assert_raise(ArgumentError){ ThreeModA.send(:===) }
      assert_raise(ArgumentError){ ThreeModA.send(:===, @obj, @obj) }
   end

   def teardown
      @obj = nil
   end
end
