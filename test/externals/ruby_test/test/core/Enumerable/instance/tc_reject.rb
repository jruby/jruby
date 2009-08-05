#########################################################################
# tc_reject.rb
#
# Test suite for the Enumerable#reject instance method.
#########################################################################
require 'test/unit'

class MyEnumReject
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

class TC_Enumerable_Reject_InstanceMethod < Test::Unit::TestCase
   def setup
      @enum = MyEnumReject.new(1,2,3)
   end

   def test_reject_basic
      assert_respond_to(@enum, :reject)
      assert_nothing_raised{ @enum.reject{} }
   end

   def test_reject
      assert_equal([1,2,3], @enum.reject{ |e| e > 7 })
      assert_equal([1], @enum.reject{ |e| e > 1 })
   end

   def test_reject_explicit_false_and_nil
      @enum = MyEnumReject.new(nil, nil, false) 
      assert_equal([false], @enum.reject{ |e| e.nil? })
      assert_equal([nil, nil], @enum.reject{ |e| e == false })
      assert_equal([nil, nil, false], @enum.reject{})
   end

   def test_reject_edge_cases
      assert_equal([], @enum.reject{ true })
      assert_equal([1,2,3], @enum.reject{})
   end

   def test_reject_expected_errors
   # No longer a valid test in 1.8.7
=begin
      assert_raise(LocalJumpError){ @enum.reject }
=end
      assert_raise(ArgumentError){ @enum.reject(5) }
   end

   def teardown
      @enum   = nil
   end
end
