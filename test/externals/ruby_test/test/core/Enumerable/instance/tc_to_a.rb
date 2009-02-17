#########################################################################
# tc_to_a.rb
#
# Test suite for the Enumerable#to_a instance method and the
# Enumerable#entries alias.
#########################################################################
require 'test/unit'

class MyEnumToA
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

class TC_Enumerable_ToA_InstanceMethod < Test::Unit::TestCase
   def setup
      @enum = MyEnumToA.new
   end

   def test_to_a_basic
      assert_respond_to(@enum, :to_a)
      assert_nothing_raised{ @enum.to_a }
      assert_kind_of(Array, @enum.to_a)
   end

   def test_to_a
      assert_equal([1, 2, 3], @enum.to_a)
   end

   def test_to_a_edge_cases
      assert_equal([nil, nil, nil], MyEnumToA.new(nil, nil, nil).to_a)
   end
   
   def test_entries_alias
      msg = '=> Known issue in MRI'
      assert_respond_to(@enum, :entries)
      assert_equal(true, @enum.method(:to_a) == @enum.method(:entries), msg)
   end

   def test_expected_errors
      assert_raise(ArgumentError){ @enum.to_a(true) }
   end

   def teardown
      @enum = nil
   end
end
