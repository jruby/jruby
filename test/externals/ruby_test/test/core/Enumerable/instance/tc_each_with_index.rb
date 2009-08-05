#########################################################################
# tc_each_with_index.rb
#
# Test suite for the Enumerable#each_with_index instance method.
#
# Note: We use a custom object here because Array and Hash have custom
# implementations.
#########################################################################
require 'test/unit'

class MyEnumEachWithIndex
   include Enumerable

   attr_accessor :arg1, :arg2, :arg3

   def initialize(arg1='a', arg2='b', arg3='c')
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

class TC_Enumerable_EachWithIndex_InstanceMethod < Test::Unit::TestCase
   def setup
      @enum  = MyEnumEachWithIndex.new
      @array = []
   end

   def test_each_with_index_basic
      assert_respond_to(@enum, :each_with_index)
      assert_nothing_raised{ @enum.each_with_index{} }
   end

   def test_each_with_index
      assert_nothing_raised{ @enum.each_with_index{ |e, i| @array[i] = e } }
      assert_equal(['a', 'b', 'c'], @array)
   end

   def test_expected_errors
   # No longer a valid test in 1.8.7
=begin
      assert_raise(LocalJumpError){ @enum.each_with_index }
=end
      assert_raise(ArgumentError){ @enum.each_with_index(true) }
   end

   def teardown
      @enum  = nil
      @array = nil
   end
end
