#######################################################################
# tc_aref.rb
#
# Test case for the Array#[] instance method.
#######################################################################
require 'test/unit'
require 'test/helper'

class TC_Array_Aref_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @empty = []
      @basic = [1,2,3]
      @multi = [1, 'foo', /^$/]
   end
   
   def test_aref_integer_index
      assert_nil(@empty[0])
      assert_nil(@empty[-1])
      assert_nil(@empty[1])
      
      assert_equal(1, @basic[0])
      assert_equal(3, @basic[2])
      assert_equal(3, @basic[-1])
      assert_nil(@basic[-5])
      assert_nil(@basic[99])
   end

   def test_aref_float_index
      assert_equal(1, @basic[0.5])
      assert_equal(3, @basic[2.2])
      assert_equal(3, @basic[-1.7])
   end
   
   def test_aref_start_and_length
      assert_equal(['foo', /^$/], @multi[1, 2])
      assert_equal([1, 'foo'], @multi[0, 2])
      assert_equal([1, 'foo'], @multi[-3, 2])     
      assert_nil(@multi[-5, 2])
   end

   def test_aref_start_and_length_with_floats
      assert_equal(['foo', /^$/], @multi[1.5, 2.5])
      assert_equal([1, 'foo'], @multi[0.3, 2.1])
      assert_equal([1, 'foo'], @multi[-3.9, 2.0])     
      assert_nil(@multi[-5.0, 2.7])
   end
   
   def test_aref_range
      assert_equal([], @empty[0..1])
      assert_equal([1,2], @basic[0..1])
      assert_equal([1,2,3], @basic[0..2])
      assert_equal([1,2,3], @basic[0..3])
      assert_equal([1,2,3], @basic[-3..-1])
   end

   def test_aref_expected_errors
      assert_raise(TypeError){ @empty[nil] }
      assert_raise(TypeError){ @empty[0, nil] }
      assert_raise(TypeError){ @empty['foo'] }
      assert_raise(TypeError, ArgumentError){ @basic[1..3, -1] } # MRI, Amber
      assert_raise(TypeError){ @basic['1'.to_sym, '2'.to_sym] }
      assert_raise(TypeError){ @basic['1'.to_sym, '2'] }
      assert_raise(TypeError){ @basic['1'.to_sym, 2] }
      assert_raise(TypeError){ @basic['1'.to_sym] }
      assert_raise(ArgumentError){ @basic[] }
   end

   def test_aref_empty_expression
      assert_equal([1, nil, 2], [1, (), 2])
      assert_equal([1, nil, 2], [1, (()), 2])
   end

   def test_aref_edge_cases
      assert_nil(@basic[1, -1])
      assert_nil(@empty[-2..-1])
      assert_nil(@empty[-1..-2])
   end
   
   def teardown
      @empty = nil
      @basic = nil
      @multi = nil
   end
end
