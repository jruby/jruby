#########################################################################
# tc_include.rb
#
# Test suite for the Enumerable#include instance method and the
# Enumerable#member? alias.
#########################################################################
require 'test/unit'

class MyEnumInclude
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

class TC_Enumerable_Include_InstanceMethod < Test::Unit::TestCase
   def setup
      @enum = MyEnumInclude.new('alpha', 7, 'beta')
   end

   def test_include_basic
      assert_respond_to(@enum, :include?)
      assert_nothing_raised{ @enum.include?(1) }
   end

   def test_include
      assert_equal(true, @enum.include?('alpha'))
      assert_equal(true, @enum.include?(7))
      assert_equal(true, @enum.include?('beta'))
      assert_equal(false, @enum.include?('7'))
      assert_equal(false, @enum.include?(8))
   end

   def test_include_explicit_false_and_nil
      @enum = MyEnumInclude.new(true, false, nil)
      assert_equal(true, @enum.include?(true))
      assert_equal(true, @enum.include?(false))
      assert_equal(true, @enum.include?(nil))
   end

   def test_member_alias
      msg = '=> Known issue in MRI'
      assert_respond_to(@enum, :member?)
      assert_equal(true, @enum.method(:member?) == @enum.method(:include?),msg)
   end

   def test_include_expected_errors
      assert_raise(ArgumentError){ @enum.include? }
      assert_raise(ArgumentError){ @enum.include?(5, 7) }
   end

   def teardown
      @enum   = nil
   end
end
