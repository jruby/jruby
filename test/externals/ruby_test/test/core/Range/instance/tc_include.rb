######################################################################
# tc_include.rb
#
# Test case for the Range#include? instance method as well as the
# Range#=== and Range#member? aliases.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_Range_Threequals_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @range1 = Range.new(-1, 1)
      @range2 = Range.new(-1, 1, true)
   end

   def test_include_basic
      assert_respond_to(@range1, :include?)
      assert_nothing_raised{ @range1.include? @range2 }
   end

   def test_threequals_alias_basic
      assert_respond_to(@range1, :===)
      assert_nothing_raised{ @range1 === @range2 }
   end

   def test_member_alias_basic
      assert_respond_to(@range1, :member?)
      assert_nothing_raised{ @range1.member? @range2 }
   end

   def test_include_inclusive
      assert_equal(true, @range1.include?(-1))
      assert_equal(true, @range1.include?(-1.0))
      assert_equal(true, @range1.include?(0))
      assert_equal(true, @range1.include?(0))
      assert_equal(true, @range1.include?(1))
      assert_equal(true, @range1.include?(0.5))

      assert_equal(false, @range1.include?(2))
      assert_equal(false, @range1.include?(-2))
      assert_equal(false, @range1.include?(-1.1))
   end

   def test_include_exclusive
      assert_equal(true, @range2.include?(-1))
      assert_equal(true, @range2.include?(-1.0))
      assert_equal(true, @range2.include?(0))
      assert_equal(true, @range2.include?(0))
      assert_equal(true, @range2.include?(0.999999))

      assert_equal(false, @range2.include?(1))
      assert_equal(false, @range1.include?(2))
      assert_equal(false, @range1.include?(-2))
      assert_equal(false, @range1.include?(-1.1))
   end

   def test_threequals_alias
      assert_equal(true, @range1 === -1)
      assert_equal(false, @range1 === 2)
      assert_equal(true, @range2 === -1)
      assert_equal(false, @range2 === 1)
   end

   def test_member_alias
      assert_equal(true, @range1.member?(-1))
      assert_equal(false, @range1.member?(2))
      assert_equal(true, @range2.member?(-1))
      assert_equal(false, @range2.member?(1))
   end

   def teardown
      @range1 = nil
      @range2 = nil
   end
end
