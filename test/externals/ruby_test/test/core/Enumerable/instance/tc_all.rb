######################################################################
# tc_all.rb
#
# Test case for the Enumerable#all? instance method.
######################################################################
require 'test/unit'

class TC_Enumerable_All_Instance < Test::Unit::TestCase
   def setup
      @enum1 = [0, 1, 2, -1]
      @enum2 = [nil, false, true]
   end

   def test_all_basic
      assert_respond_to(@enum1, :all?)
      assert_nothing_raised{ @enum1.all? }
      assert_nothing_raised{ @enum1.all?{ } }
   end

   def test_all_return_value
      assert_equal(true, @enum1.all?)
      assert_equal(true, @enum1.all?{ |o| o < 5 })

      assert_equal(false, @enum1.all?{ |o| o > 2 })
      assert_equal(false, @enum2.all?)
   end

   def test_all_edge_cases
      assert_nothing_raised{ @enum1.all?{ |o| 5.times{ @enum1.shift } } }
   end

   def test_all_expected_failures
      assert_raises(ArgumentError){ @enum1.all?(1) }
   end

   def teardown
      @enum1 = nil
   end
end
