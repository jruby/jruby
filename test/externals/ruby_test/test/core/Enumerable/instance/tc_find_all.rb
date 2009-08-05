#########################################################################
# tc_find_all.rb
#
# Test suite for the Enumerable#find_all instance method and the
# Enumerable#select alias.
#########################################################################
require 'test/unit'

class MyEnumFindAll
   include Enumerable

   attr_accessor :arg1, :arg2, :arg3

   def initialize(arg1, arg2, arg3)
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

class TC_Enumerable_FindAll_InstanceMethod < Test::Unit::TestCase
   def setup
      @enum = MyEnumFindAll.new(1,2,3)
   end

   def test_find_all_basic
      assert_respond_to(@enum, :find_all)
      assert_nothing_raised{ @enum.find_all{} }
   end

   def test_find_all
      assert_equal([2,3], @enum.find_all{ |e| e > 1 })
      assert_equal([], @enum.find_all{ |e| e > 7 })
   end

   def test_find_all_explicit_false_and_nil
      @enum = MyEnumFindAll.new(nil, nil, false) 
      assert_equal([nil, nil], @enum.find_all{ |e| e.nil? })
      assert_equal([false], @enum.find_all{ |e| e == false })
      assert_equal([], @enum.find_all{})
   end

   def test_find_all_edge_cases
      assert_equal([1,2,3], @enum.find_all{ true })
      assert_equal([], @enum.find_all{})
   end

   # No longer a valid test in 1.8.7
=begin
   def test_select_alias
      msg = '=> Known issue in MRI'
      assert_respond_to(@enum, :select)
      assert_equal(true, @enum.method(:find_all) == @enum.method(:select), msg)
   end
=end

   def test_find_all_expected_errors
   # No longer a valid test in 1.8.7
=begin
      assert_raise(LocalJumpError){ @enum.find_all }
=end
      assert_raise(ArgumentError){ @enum.find_all(5) }
   end

   def teardown
      @enum   = nil
   end
end
