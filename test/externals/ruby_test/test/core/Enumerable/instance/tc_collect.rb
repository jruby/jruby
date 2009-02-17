#########################################################################
# tc_collect.rb
#
# Test suite for the Enumerable#collect instance method and the
# Enumerable#map alias.
#
# Note: I've created my own class here because other classes tend to
# implement their own custom version of collect/map.
#########################################################################
require 'test/unit'

class MyEnumCollect
   include Enumerable

   attr_accessor :arg1, :arg2, :arg3

   def initialize(arg1=1, arg2=2, arg3=3)
      @arg1 = arg1
      @arg2 = arg2
      @arg3 = arg3
   end

   def each
      yield @arg1
      yield @arg2
      yield @arg3
   end
end

class TC_Enumerable_Collect_Instance < Test::Unit::TestCase
   def setup
      @enum = MyEnumCollect.new
   end

   def test_collect_basic
      assert_respond_to(@enum, :collect)
      assert_nothing_raised{ @enum.collect }
   end

   def test_collect
      assert_equal([2,3,4], @enum.collect{ |e| e += 1 })
      assert_equal([7,7,7], @enum.collect{ 7 })
   end

   def test_collect_edge_cases
      assert_equal(['a','a','a'], @enum.collect{ 'a' })
      assert_equal([nil, nil, nil], @enum.collect{})
      assert_equal([1, 2, 3], @enum.collect)
   end

   def test_map_alias
      msg = '=> Known issue in MRI'
      assert_respond_to(@enum, :map)
      assert_equal(true, @enum.method(:collect) == @enum.method(:map), msg) 
   end

   def test_collect_expected_errors
      assert_raise(ArgumentError){ @enum.collect(5) }
   end

   def teardown
      @enum = nil
   end
end
