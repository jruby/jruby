########################################################################
# tc_partition.rb
#
# Test case for the Enumerable#partition instance method.
########################################################################
require 'test/unit'

class TC_Enumerable_Partition_InstanceMethod < Test::Unit::TestCase
   def setup
      @enum = [1,2,3,4,5]
   end

   def test_partition_basic
      assert_respond_to(@enum, :partition)
      assert_nothing_raised{ @enum.partition{} }
      assert_equal(2, @enum.partition{}.length)
      assert_kind_of(Array, @enum.partition{}[0])
      assert_kind_of(Array, @enum.partition{}[1])
   end

   def test_partition
      assert_equal([[1,3,5],[2,4]], @enum.partition{ |e| e % 2 != 0 })
      assert_equal([[1,2,3,4,5],[]], @enum.partition{ |e| e < 10 })
      assert_equal([[],[1,2,3,4,5]], @enum.partition{ |e| e > 10 })
   end

   def test_partition_edge_cases
      assert_equal([[],[]], [].partition{ |e| e > 10 })
      assert_equal([[nil],[false]], [nil,false].partition{ |e| e.nil? })
      assert_equal([[false],[nil]], [nil,false].partition{ |e| e == false })
      assert_equal([[],[nil, false]], [nil,false].partition{})
   end

   def test_partition_expected_errors
   # No longer a valid test in 1.8.7
=begin
      assert_raise(LocalJumpError){ @enum.partition }
=end
      assert_raise(ArgumentError){ @enum.partition(true) }
   end

   def teardown
      @enum = nil
   end
end
